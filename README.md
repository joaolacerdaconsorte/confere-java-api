# Confere — API Java (Global Solution 2026/2 · CitizenTech)

> **Disciplina:** Java Advanced | **Turma:** 2TDSPW | **Professor:** Leonardo Real (`lgsreal`)  
> **Tema:** CitizenTech — Tecnologia com o cidadão no centro (*"Antes de clicar, pagar ou desistir de um direito: confere."*)

API RESTful em **Java 21 + Spring Boot 3.4.5**, responsável pelo módulo de **contas e acessibilidade, consentimentos LGPD, verificações inteligentes (com Spring AI + cliente do serviço de IA), rede de guardiões familiares e mensageria assíncrona com RabbitMQ**.

---

## Integrantes do Grupo (2TDSPW)

| Nome Completo | RM | Turma |
|---|---|---|
| **João Vitor Lacerda Consorte** | **565565** | 2TDSPW |
| **Kauan Vieira de Lima** | **565403** | 2TDSPW |
| **Murillo Fernandes Carapia** | **564969** | 2TDSPW |
| **Pedro de Matos Previtali** | **564184** | 2TDSPW |

---

## Arquitetura e Integração

```mermaid
flowchart LR
    APP["App Mobile (Expo / React Native)"] -->|JWT Bearer| JAVA["API Java Spring Boot (:8080)"]
    JAVA -->|POST /v1/analises (X-Api-Key)| IA["Serviço de IA Python (:8000)"]
    JAVA -->|ChatClient (Spring AI)| LLM["LLM (Explicador Acessível RN05)"]
    JAVA -->|AFTER_COMMIT| RMQ["RabbitMQ (confere.verificacoes)"]
    RMQ -->|verificacao.risco| L1["AlertaGuardiaoListener (RN07)"]
    RMQ -->|verificacao.offline| L2["VerificacaoOfflineListener (RN08)"]
    JAVA -->|JPA| DB["Oracle (11 tabelas CF_*) / H2 Dev"]
    APP -->|Mesmo JWT HS256| DOTNET["API .NET Minimal API (:5080)"]
```

### Opção Escolhida no Requisito 5 (Arquitetura Distribuída)
- **Opção A — RabbitMQ (Mensageria Assíncrona):**
  - **Exchange (Topic):** `confere.verificacoes`
  - **Fila de Risco:** `confere.verificacao.risco` (routing key `verificacao.risco`, DLQ `confere.verificacao.risco.dlq`) → consumida por `AlertaGuardiaoListener` (RN07).
  - **Fila Offline:** `confere.verificacao.offline` (routing key `verificacao.offline`, DLQ `confere.verificacao.offline.dlq`) → consumida por `VerificacaoOfflineListener` (RN08).
  - **Publicação Transacional:** `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` garante que eventos só são publicados após o commit no banco (RN06).

---

## Como Rodar

### 1. Execução Local Rápida (Perfil `dev` com H2 em memória — zero dependências externas)
Pré-requisitos: **Java 21 LTS**.

```powershell
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080** com seed automático (municípios, canais, 10 tipos de golpe, regras offline e usuários de teste).
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **H2 Console (dev):** http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:conferedb`, usuário `sa`, sem senha)

### 2. Execução Completa com Docker Compose (API + RabbitMQ + Oracle opcional)
```bash
cp .env.example .env
# Subir API + RabbitMQ (perfil dev)
docker compose up --build

# Subir com Oracle XE (perfil oracle)
docker compose --profile oracle up --build
```

### 3. Executar Testes Automatizados
```powershell
.\mvnw.cmd test
```

### 4. Rodar com o RabbitMQ consumindo as filas (Requisito 5)
No perfil `dev` os consumidores ficam desligados para a API subir sem dependências. Para ver o evento ser publicado **e consumido**:

```powershell
docker compose up -d rabbitmq
$env:RABBITMQ_AUTO_STARTUP = "true"
.\mvnw.cmd spring-boot:run
```

- Painel do RabbitMQ: http://localhost:15672 (guest / guest) → aba **Queues** mostra `confere.verificacao.risco` e `confere.verificacao.offline`.
- Uma verificação com veredito `GOLPE` publica em `verificacao.risco`; o `AlertaGuardiaoListener` consome e grava o alerta, que aparece em `GET /api/alertas` para o guardião (`joao@confere.app`).
- Uma verificação com `"modo": "OFFLINE"` responde `202` e é processada pelo `VerificacaoOfflineListener`.

No `docker compose up --build` completo, os consumidores já sobem ligados (`RABBITMQ_AUTO_STARTUP=true`).

### 5. Teste de ponta a ponta de todos os endpoints
Com a API no ar (local ou na nuvem), o script percorre login, cadastro, perfil, consentimentos, verificações, guardiões, alertas e o CRUD de administração, conferindo status HTTP e o formato dos erros:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\testar-api.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\testar-api.ps1 -BaseUrl https://<sua-api>.azurewebsites.net
```

### 6. Spring AI com um LLM de verdade
Sem chave, o explicador usa a resposta segura padrão por veredito (nunca devolve 500). Para gerar as frases com o LLM:

| Variável | Google Gemini (gratuito pelo Google AI Studio) | OpenAI |
|---|---|---|
| `LLM_API_KEY` | chave criada em aistudio.google.com | chave `sk-...` |
| `LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` | `https://api.openai.com` (padrão) |
| `LLM_COMPLETIONS_PATH` | `/chat/completions` | `/v1/chat/completions` (padrão) |
| `LLM_MODEL` | `gemini-3.8-flash` | `gpt-4o-mini` (padrão) |

O Gemini é usado pela camada compatível com a API da OpenAI, então o código continua no `ChatClient` do Spring AI sem nenhuma mudança.

---

## Usuários de Teste (Seed)

| E-mail | Senha | Papel (JWT) | Descrição |
|---|---|---|---|
| `cida@confere.app` | `Senha@123` | `CIDADAO` | Maria Aparecida (idosa, fonte grande e áudio ativos, consentimentos aceitos) |
| `joao@confere.app` | `Senha@123` | `CIDADAO` | João Aparecido (guardião ATIVO de Maria Aparecida, com push token) |
| `admin@confere.app` | `Senha@123` | `ADMIN` | Administrador dos catálogos de tipos de golpe e regras offline |

---

## Endpoints Principais (Prefixo `/api`)

| Método | Rota | Descrição | Papel |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | Cadastro de cidadão (201) | Público |
| `POST` | `/api/auth/login` | Login e emissão de JWT HS256 (200) | Público |
| `GET · PUT` | `/api/usuarios/me` | Perfil e acessibilidade (`fonteGrande`, `audioAtivo`) | `CIDADAO` |
| `PUT` | `/api/usuarios/me/push-token` | Salva `expoPushToken` do aparelho | `CIDADAO` |
| `GET · POST` | `/api/consentimentos` | Lista e registra aceite LGPD por finalidade | `CIDADAO` |
| `POST` | `/api/verificacoes` | Nova verificação JSON ou multipart (201 online / 202 offline) | `CIDADAO` |
| `GET` | `/api/verificacoes` | Histórico paginado com filtro por `veredito` (RN09) | `CIDADAO` |
| `GET · DELETE` | `/api/verificacoes/{id}` | Detalhe e exclusão física em cascata (RN09) | `CIDADAO` |
| `PUT` | `/api/verificacoes/{id}/feedback` | Cria ou atualiza feedback do desfecho | `CIDADAO` |
| `GET · POST` | `/api/guardioes` | Lista guardiões / convida familiar por e-mail (RN10) | `CIDADAO` |
| `PUT · DELETE` | `/api/guardioes/{idVinculo}` | Edita parentesco/nível ou revoga vínculo | `CIDADAO` |
| `GET` | `/api/guardioes/convites` | Convites recebidos (`PENDENTE`) | `CIDADAO` |
| `POST` | `/api/guardioes/{idVinculo}/aceitar` · `/recusar` | Resposta do convidado (RN10) | `CIDADAO` |
| `GET` | `/api/protegidos` | Pessoas protegidas (`ATIVO`) | `CIDADAO` |
| `GET · PATCH` | `/api/alertas` · `/api/alertas/{id}` | Alertas dos protegidos e marcação `LIDO`/`RESOLVIDO` | `CIDADAO` |
| `GET` | `/api/catalogos/canais` · `/tipos-golpe` | Catálogos para o app | `CIDADAO` |
| `GET` | `/api/regras-offline?versao=` | Regras ativas do modo offline (`304` se inalterado) | `CIDADAO` |
| `CRUD` | `/api/admin/tipos-golpe` · `/regras-offline` | Manutenção dos catálogos | `ADMIN` |
| `GET` | `/actuator/health` | Saúde da API | Público |

---

## Regras de Negócio Implementadas

- **RN01 (Mascaramento):** `MascaramentoService` mascara CPF (`[CPF]`), cartão (`[CARTAO]`), agência/conta (`[CONTA]`), telefone (`[TELEFONE]`) e e-mail (`[EMAIL]`) antes de salvar ou enviar à IA, persistindo apenas `HASH_CONTEUDO` (SHA-256) do original.
- **RN02 (Consentimento):** `POST /api/verificacoes` exige consentimento `ANALISE_IA` aceito; sem ele retorna HTTP `403` com `codigo: "CONSENTIMENTO_PENDENTE"`.
- **RN03 e RN04 (Score e Veredito):** `INCONCLUSIVO` se confiança OCR `< 0.5` ou texto `< 15` chars; `GOLPE` (`>= 70`), `SUSPEITO` (`40..69`), `OFICIAL` ou `DIREITO` (`< 40`).
- **RN05 (Linguagem Acessível com Spring AI):** `ExplicadorAcessivelService` usa `ChatClient` com saída estruturada `ExplicacaoAcessivel` (máx. 3 frases curtas, sem jargão, termina com canal oficial, nunca usa `"100% seguro"` ou `"com certeza"`), com fallback seguro caso o LLM falhe.
- **RN06 e RN07 (Eventos e Alertas):** Publicação pós-commit para RabbitMQ e criação de `CF_ALERTA_GUARDIAO` respeitando `NIVEL_MINIMO`.
- **RN08 (Modo Offline):** Verificações offline retornam `202 Accepted` e são processadas pela fila `confere.verificacao.offline`.
- **RN09 (Privacidade LGPD):** Cada cidadão só acessa suas próprias verificações e pode excluí-las em cascata.
- **RN10 (Guardiões):** Limite de 5 guardiões ativos, bloqueio de auto-convite e convite duplicado (`409 GUARDIAO_DUPLICADO`).

---

## Variáveis de Ambiente

| Variável | Uso | Padrão no perfil `dev` |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (H2 em memória) ou `oracle` | `dev` |
| `JWT_SECRET` | Chave HS256 (mín. 32 bytes), **a mesma da API .NET**. Obrigatória no perfil `oracle` | chave de desenvolvimento |
| `DB_URL` · `DB_USER` · `DB_PASSWORD` | Conexão Oracle (perfil `oracle`) | — |
| `RABBITMQ_HOST` · `RABBITMQ_PORT` · `RABBITMQ_USER` · `RABBITMQ_PASSWORD` · `RABBITMQ_VHOST` · `RABBITMQ_SSL` | Broker RabbitMQ | `localhost` · `5672` · `guest` · `guest` · `/` · `false` |
| `RABBITMQ_AUTO_STARTUP` | Liga os consumidores das filas | `false` |
| `IA_BASE_URL` · `IA_API_KEY` | Serviço de IA em Python | `http://localhost:8000` |
| `IA_TIMEOUT_SECONDS` | Tempo máximo de espera pelo serviço de IA em Python | `20` |
| `OCR_TIMEOUT_SECONDS` | Tempo máximo da leitura de imagem pelo Gemini (usada quando o serviço Python não lê a imagem) | `20` |
| `LLM_API_KEY` · `LLM_MODEL` · `LLM_BASE_URL` · `LLM_COMPLETIONS_PATH` | Spring AI (OpenAI ou Gemini) | desligado |
| `LLM_TIMEOUT` | Tempo máximo de espera pelo LLM (depois disso usa a resposta padrão) | `25s` |
| `H2_CONSOLE_ENABLED` | Console do H2. **Use `false` em qualquer ambiente público** | `true` |

## Tratamento de Erros

Todas as respostas de erro seguem o **ProblemDetail (RFC 7807)** com as extensões `codigo` e `erros`, gerado pelo `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "type": "https://confere.app/erros/validacao",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/guardioes",
  "codigo": "VALIDACAO",
  "erros": [ { "campo": "emailGuardiao", "mensagem": "E-mail inválido" } ]
}
```

| Código | HTTP | Quando |
|---|---|---|
| `VALIDACAO` | 400 | Bean Validation, JSON malformado, parâmetro ou parte do multipart inválidos |
| `NAO_AUTENTICADO` | 401 | Token ausente, inválido ou login incorreto |
| `SEM_PERMISSAO` / `CONSENTIMENTO_PENDENTE` | 403 | Papel sem acesso / sem aceite `ANALISE_IA` |
| `NAO_ENCONTRADO` | 404 | Registro ou rota inexistente |
| `CONFLITO` / `GUARDIAO_DUPLICADO` | 409 | E-mail já cadastrado, convite duplicado, registro em uso |
| `ERRO_INTERNO` | 500 | Falha inesperada (sem stack trace na resposta) |

## Deploy na Azure

A API roda num **Azure Web App** (App Service Linux, Java 21, plano F1 gratuito), criado e publicado pelo script `scripts/deploy-azure.ps1` com a Azure CLI:

```powershell
az login
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-azure.ps1
```

O script gera o `.jar`, cria o Resource Group, o plano e o Web App, liga HTTPS only, grava as variáveis de ambiente (a `JWT_SECRET` e a chave do Gemini são digitadas na hora, sem aparecer na tela) e publica o pacote. Rodar de novo atualiza a API com o código atual.

- **Swagger na nuvem:** https://confere-java-rm565565.azurewebsites.net/swagger-ui.html
- **Health check:** https://confere-java-rm565565.azurewebsites.net/actuator/health
- No plano F1 o app "dorme" depois de um tempo parado: a primeira chamada depois disso pode levar até 1 minuto, e como o perfil `dev` usa H2 em memória, os dados voltam ao seed inicial quando o app reinicia.

## Deploy e Vídeo Pitch

- **Repositório GitHub:** https://github.com/joaolacerdaconsorte/confere-java-api
- **Vídeo Pitch (até 5 min):** *A ser gravado/anexado no Portal do Aluno seguindo o roteiro da Seção 16 (0:00 problema → 0:45 solução → 1:30 demonstração → 3:00 negócio → 4:00 impacto e ODS 10, 4, 11 e 9).*

---

## Uso de IA Generativa

Conforme exigido pelo edital da Global Solution:
- **Ferramentas utilizadas:** Gemini / Antigravity CLI (apoio à estruturação do projeto Spring Boot, geração de casos de teste JUnit 5/MockMvc, revisão de contratos OpenAPI/ProblemDetail e documentação). Claude (Anthropic): revisão final do tratamento global de erros, configuração do RabbitMQ e do LLM por variáveis de ambiente, timeouts do cliente do serviço de IA e documentação de execução e deploy.
- **Aplicação na solução (Spring AI):** O projeto integra o `ChatClient` do Spring AI no serviço `ExplicadorAcessivelService` para traduzir o diagnóstico técnico do motor de análise em explicações curtas, acessíveis e com roteiro de áudio para idosos (RN05).
- **Revisão humana:** Todo o código, mapeamento das 11 tabelas `CF_*`, regras de negócio (`RN01`–`RN10`) e testes automatizados foram revisados, compilados e validados localmente pelo grupo.
