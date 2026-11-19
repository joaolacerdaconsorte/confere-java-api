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
- **H2 Console (dev):** http://localhost:8080/h2-console

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

## Deploy e Vídeo Pitch

- **Repositório GitHub:** https://github.com/joaolacerdaconsorte/confere-java-api
- **Vídeo Pitch (até 5 min):** *A ser gravado/anexado no Portal do Aluno seguindo o roteiro da Seção 16 (0:00 problema → 0:45 solução → 1:30 demonstração → 3:00 negócio → 4:00 impacto e ODS 10, 4, 11 e 9).*

---

## Uso de IA Generativa

Conforme exigido pelo edital da Global Solution:
- **Ferramentas utilizadas:** Gemini / Antigravity CLI (apoio à estruturação do projeto Spring Boot, geração de casos de teste JUnit 5/MockMvc, revisão de contratos OpenAPI/ProblemDetail e documentação).
- **Aplicação na solução (Spring AI):** O projeto integra o `ChatClient` do Spring AI no serviço `ExplicadorAcessivelService` para traduzir o diagnóstico técnico do motor de análise em explicações curtas, acessíveis e com roteiro de áudio para idosos (RN05).
- **Revisão humana:** Todo o código, mapeamento das 11 tabelas `CF_*`, regras de negócio (`RN01`–`RN10`) e testes automatizados foram revisados, compilados e validados localmente pelo grupo.
