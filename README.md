# Confere - API Java

Backend em Java 21 com Spring Boot 3.4.5 do projeto **Confere**, feito pela nossa equipe da turma **2TDSPW** para a **Global Solution 2026/2 da FIAP** (tema CitizenTech, disciplina Java Advanced, prof. Leonardo Real).

O Confere ajuda idosos e pessoas com pouco hábito digital a conferir mensagens, prints e boletos antes de clicar, pagar ou abrir mão de um direito. O usuário envia um texto ou imagem, a API mascara dados pessoais antes de qualquer processamento, consulta o motor de análise (com fallback local quando o serviço Python não responde), gera uma explicação em linguagem simples pelo Spring AI e dispara alertas para os familiares guardiões via RabbitMQ.

Esta API cuida de contas, acessibilidade, consentimentos LGPD, verificações e rede de guardiões. A API .NET do grupo (`Confere-GS`) cuida do radar comunitário, elegibilidade de direitos e painel do gestor público, aceitando o mesmo token JWT emitido aqui.

## Integrantes (2TDSPW)

| Nome completo | RM | Turma |
|---|---|---|
| João Vitor Lacerda Consorte | 565565 | 2TDSPW |
| Kauan Vieira de Lima | 565403 | 2TDSPW |
| Murillo Fernandes Carapia | 564969 | 2TDSPW |
| Pedro de Matos Previtali | 564184 | 2TDSPW |

## API publicada na Azure

A API está rodando em um Azure Web App (Linux, Java 21, plano F1) e integrada ao app Mobile e ao `/health/ready` da API .NET:

- **Swagger UI:** https://confere-java-rm564969.azurewebsites.net/swagger-ui.html
- **Health check:** https://confere-java-rm564969.azurewebsites.net/actuator/health

Como roda no plano F1 gratuito, o app entra em modo ocioso após algum tempo sem requisições; a primeira chamada depois disso pode levar cerca de 1 minuto para acordar. No perfil `dev` (usado no F1 com H2 em memória), os dados voltam ao seed inicial quando o container reinicia.

Para publicar ou atualizar pela Azure CLI:

```powershell
az login
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-azure.ps1
```

## Arquitetura e requisito 5 (RabbitMQ)

```mermaid
flowchart LR
    APP["App Mobile (Expo)"] -->|JWT Bearer| JAVA["API Java Spring Boot (:8080)"]
    JAVA -->|POST /v1/analises| IA["Serviço IA Python (:8000)"]
    JAVA -->|ChatClient (Spring AI)| LLM["LLM - Gemini / OpenAI (RN05)"]
    JAVA -->|AFTER_COMMIT| RMQ["RabbitMQ (confere.verificacoes)"]
    RMQ -->|verificacao.risco| L1["AlertaGuardiaoListener (RN07)"]
    RMQ -->|verificacao.offline| L2["VerificacaoOfflineListener (RN08)"]
    JAVA -->|Spring Data JPA| DB["Oracle (11 tabelas CF_*) / H2 dev"]
    APP -->|Mesmo JWT HS256| DOTNET["API .NET (:5080)"]
```

**Opção escolhida no requisito 5:** **Opção A (RabbitMQ)**.

- **Exchange:** `confere.verificacoes` (tipo `topic`).
- **Fila de risco:** `confere.verificacao.risco` (routing key `verificacao.risco`, dead-letter `confere.verificacao.risco.dlq`). Quando uma verificação fecha como `GOLPE` ou `SUSPEITO`, o `VerificacaoEventPublisher` publica `VerificacaoRiscoEvent` com `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` (RN06). O `AlertaGuardiaoListener` consome a fila e cria registros em `CF_ALERTA_GUARDIAO` para cada vínculo `ATIVO` respeitando o `NIVEL_MINIMO` configurado (RN07).
- **Fila offline:** `confere.verificacao.offline` (routing key `verificacao.offline`, dead-letter `confere.verificacao.offline.dlq`). Envios com `"modo": "OFFLINE"` respondem `202 Accepted` e são processados de forma assíncrona pelo `VerificacaoOfflineListener` (RN08).

## Como rodar localmente

Pré-requisito: **Java 21**. Não precisa instalar Maven nem subir banco para testar no modo padrão (`dev`, com H2 em modo de compatibilidade Oracle).

```powershell
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

Serviços locais:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/actuator/health
- **H2 Console (perfil dev):** http://localhost:8080/h2-console (`jdbc:h2:mem:conferedb`, usuário `sa`, senha vazia)

### Rodar com RabbitMQ consumindo as filas

No perfil `dev`, o consumo das filas começa desligado para a API subir sem broker local. Para ver publicação e consumo funcionando de ponta a ponta:

```powershell
docker compose up -d rabbitmq
$env:RABBITMQ_AUTO_STARTUP = "true"
.\mvnw.cmd spring-boot:run
```

O painel do RabbitMQ fica em http://localhost:15672 (`guest` / `guest`).

### Subir com Docker Compose (API + RabbitMQ + Oracle opcional)

```bash
cp .env.example .env
# API + RabbitMQ (perfil dev, consumidores ligados)
docker compose up --build

# Com Oracle XE (perfil oracle)
docker compose --profile oracle up --build
```

### Rodar testes automatizados

```powershell
.\mvnw.cmd test
```

Atualmente a suíte tem **19 testes** (unitários de regras de negócio, cliente de IA/imagem e integração HTTP com `MockMvc` e H2) rodando também no GitHub Actions a cada push.

Para testar todos os endpoints contra a API local ou publicada na Azure:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\testar-api.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\testar-api.ps1 -BaseUrl https://confere-java-rm564969.azurewebsites.net
```

## Spring AI (explicador acessível)

Dentro de `POST /api/verificacoes`, o `ExplicadorAcessivelService` usa o `ChatClient` do Spring AI com saída estruturada (`.entity(ExplicacaoAcessivel.class)`) para gerar até 3 frases curtas, o roteiro de áudio, a mensagem para o guardião e o próximo passo, sempre citando um canal oficial e evitando expressões como "100% seguro" ou "com certeza" (RN05).

Se o LLM demorar mais que o timeout configurado ou estiver sem chave, a API devolve um texto padrão por veredito e nunca retorna 500. Quando o serviço Python de IA está fora do ar e o usuário envia uma foto, o `LeitorImagemService` também usa o Gemini para extrair o texto da imagem.

| Variável | Google Gemini (Google AI Studio) | OpenAI |
|---|---|---|
| `LLM_API_KEY` | chave do aistudio.google.com | chave `sk-...` |
| `LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` | `https://api.openai.com` (padrão) |
| `LLM_COMPLETIONS_PATH` | `/chat/completions` | `/v1/chat/completions` (padrão) |
| `LLM_MODEL` | `gemini-3.8-flash` | `gpt-4o-mini` (padrão) |

## Usuários de teste (seed automático)

| E-mail | Senha | Papel | Perfil no seed |
|---|---|---|---|
| `cida@confere.app` | `Senha@123` | `CIDADAO` | Maria Aparecida (fonte grande e áudio ativos, consentimentos aceitos) |
| `joao@confere.app` | `Senha@123` | `CIDADAO` | João Aparecido (guardião ATIVO de Maria Aparecida, com push token) |
| `admin@confere.app` | `Senha@123` | `ADMIN` | Administrador dos catálogos de golpes e regras offline |

## Endpoints principais (prefixo `/api`)

| Método | Rota | Descrição | Papel |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | Cadastro de cidadão (201) | Público |
| `POST` | `/api/auth/login` | Login com retorno de JWT HS256 (200) | Público |
| `GET · PUT` | `/api/usuarios/me` | Consulta e atualização de perfil e acessibilidade | `CIDADAO` |
| `PUT` | `/api/usuarios/me/push-token` | Grava token de push do aparelho | `CIDADAO` |
| `GET · POST` | `/api/consentimentos` | Histórico e registro de aceite LGPD por finalidade | `CIDADAO` |
| `POST` | `/api/verificacoes` | Nova verificação em JSON ou multipart (`dados` + `imagem`) | `CIDADAO` |
| `GET` | `/api/verificacoes` | Histórico paginado com filtro por `veredito` (RN09) | `CIDADAO` |
| `GET · DELETE` | `/api/verificacoes/{id}` | Detalhe e exclusão física em cascata (RN09) | `CIDADAO` |
| `PUT` | `/api/verificacoes/{id}/feedback` | Cria ou atualiza feedback do desfecho | `CIDADAO` |
| `GET · POST` | `/api/guardioes` | Lista guardiões e envia convite por e-mail (RN10) | `CIDADAO` |
| `PUT · DELETE` | `/api/guardioes/{idVinculo}` | Edita parentesco/nível ou revoga vínculo | `CIDADAO` |
| `GET` | `/api/guardioes/convites` | Convites recebidos com status `PENDENTE` | `CIDADAO` |
| `POST` | `/api/guardioes/{idVinculo}/aceitar` · `/recusar` | Resposta do convidado | `CIDADAO` |
| `GET` | `/api/protegidos` | Lista de protegidos (`ATIVO`) | `CIDADAO` |
| `GET · PATCH` | `/api/alertas` · `/api/alertas/{id}` | Alertas dos protegidos e atualização para `LIDO`/`RESOLVIDO` | `CIDADAO` |
| `GET` | `/api/catalogos/canais` · `/tipos-golpe` | Catálogos usados pelo app | `CIDADAO` |
| `GET` | `/api/regras-offline?versao=` | Regras ativas do modo offline (`304` se a versão não mudou) | `CIDADAO` |
| `CRUD` | `/api/admin/tipos-golpe` · `/regras-offline` | Manutenção dos catálogos | `ADMIN` |
| `GET` | `/actuator/health` | Saúde da API | Público |

## Regras de negócio implementadas

- **RN01:** `MascaramentoService` substitui CPF (`[CPF]`), cartão (`[CARTAO]`), agência/conta (`[CONTA]`), telefone (`[TELEFONE]`) e e-mail (`[EMAIL]`) antes de gravar ou chamar o LLM; só o hash SHA-256 (`HASH_CONTEUDO`) do texto original é persistido.
- **RN02:** `POST /api/verificacoes` exige consentimento `ANALISE_IA` aceito; sem ele devolve `403` com `codigo: "CONSENTIMENTO_PENDENTE"`.
- **RN03 e RN04:** score de 0 a 100; veredito `INCONCLUSIVO` para OCR < 0,5 ou texto com menos de 15 caracteres; `GOLPE` (>= 70), `SUSPEITO` (40 a 69), `OFICIAL` ou `DIREITO` (< 40).
- **RN05:** explicação com no máximo 3 frases curtas, canal oficial no final e sem promessa absoluta ("100% seguro" ou "com certeza").
- **RN06 e RN07:** publicação de evento pós-commit e geração de alertas para vínculos ativos conforme `NIVEL_MINIMO`.
- **RN08:** requisições com `modo: "OFFLINE"` respondem `202` e vão para a fila `confere.verificacao.offline`.
- **RN09:** o usuário só enxerga as próprias verificações e pode excluí-las em cascata (sinais, alertas e feedback).
- **RN10:** limite de 5 guardiões ativos, bloqueio de auto-convite e erro `409 GUARDIAO_DUPLICADO` para convites repetidos.

## Variáveis de ambiente

| Variável | Uso | Padrão no perfil `dev` |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (H2) ou `oracle` | `dev` |
| `JWT_SECRET` | Chave HS256 (mín. 32 bytes), compartilhada com a API .NET | chave local de dev |
| `DB_URL` · `DB_USER` · `DB_PASSWORD` | Conexão Oracle (`oracle`) | — |
| `RABBITMQ_HOST` · `RABBITMQ_PORT` · `RABBITMQ_USER` · `RABBITMQ_PASSWORD` · `RABBITMQ_VHOST` · `RABBITMQ_SSL` | Broker RabbitMQ | `localhost` · `5672` · `guest` · `guest` · `/` · `false` |
| `RABBITMQ_AUTO_STARTUP` | Ativa os listeners RabbitMQ | `false` |
| `IA_BASE_URL` · `IA_API_KEY` | Serviço de IA em Python | `http://localhost:8000` |
| `IA_TIMEOUT_SECONDS` · `OCR_TIMEOUT_SECONDS` | Limites de espera do serviço Python e da leitura de imagem | `20` · `20` |
| `LLM_API_KEY` · `LLM_MODEL` · `LLM_BASE_URL` · `LLM_COMPLETIONS_PATH` · `LLM_TIMEOUT` | Spring AI (Gemini ou OpenAI) | desligado · `gpt-4o-mini` · `25s` |
| `H2_CONSOLE_ENABLED` | Console H2 (`false` em produção) | `true` |

## Tratamento de erros

Os erros seguem o padrão **ProblemDetail (RFC 7807)** com `codigo` e `erros`, centralizado no `GlobalExceptionHandler` (`@RestControllerAdvice`):

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

Códigos usados: `VALIDACAO` (400), `NAO_AUTENTICADO` (401), `SEM_PERMISSAO` / `CONSENTIMENTO_PENDENTE` (403), `NAO_ENCONTRADO` (404), `CONFLITO` / `GUARDIAO_DUPLICADO` (409) e `ERRO_INTERNO` (500, sem expor stack trace).

## Demonstração e vídeo pitch

Por conflito de horários de trabalho e fechamento das outras entregas na reta final, não conseguimos gravar o vídeo pitch separado de 5 minutos desta disciplina. Deixamos:

- **Demonstração da API Java funcionando integrada ao app Mobile:** https://youtu.be/lqc-IkzOcis (mostra cadastro, login JWT, conferência de mensagem, explicação com áudio, feedback, histórico, exclusão e consentimento LGPD rodando contra a API Java publicada na Azure).
- **Roteiro do pitch para a apresentação presencial:** [docs/roteiro-pitch.md](docs/roteiro-pitch.md).
- **Swagger online pronto para teste:** https://confere-java-rm564969.azurewebsites.net/swagger-ui.html

## Uso de IA generativa

Usamos Gemini e Claude como apoio no desenvolvimento: estruturação inicial das camadas Spring Boot, apoio na configuração do Spring AI e do RabbitMQ, revisão do tratamento global de erros, escrita de testes automatizados e revisão do README. Todo o código, o mapeamento das 11 tabelas `CF_*`, os scripts de banco e os testes foram revisados, executados e validados pelo grupo. Na própria aplicação, o Spring AI (`ChatClient`) atua no `ExplicadorAcessivelService` para gerar explicações acessíveis para idosos.
