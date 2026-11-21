# Dev. Web — Perguntas e Respostas com IA (Gateway + RabbitMQ + Worker + React)

```
Dev. Web/
├── docker-compose.yml      RabbitMQ (AMQP 5672 / painel 15672)
├── gateway/                Spring Boot Web: JWT, POST /api/perguntas, GET /api/stream (SSE)
├── worker/                 Spring Boot SEM web: consome fila, Spring AI, publica resposta
└── frontend/               React (Vite) + @microsoft/fetch-event-source
```

## Pré-requisitos (Windows)
- JDK 17+, Maven 3.9+ (ou abra as pastas `gateway` e `worker` no IntelliJ)
- Node.js 20+
- Docker Desktop
- Uma chave da OpenAI (ou Ollama local, veja abaixo)

## Como rodar (PowerShell, 4 janelas)
```powershell
cd "C:\caminho\para\Dev. Web"

# 1) RabbitMQ
docker compose up -d

# 2) Gateway  (http://localhost:8080)
cd gateway
mvn spring-boot:run

# 3) Worker
cd ..\worker
$env:OPENAI_API_KEY = "sk-..."
mvn spring-boot:run

# 4) Front-end (http://localhost:5173)
cd ..\frontend
npm install
npm run dev
```
Abra http://localhost:5173, crie uma conta e pergunte. Painel do RabbitMQ: http://localhost:15672 (admin/admin).

## Usar Ollama (modelo local, sem custo)
1. Em `worker/pom.xml`, troque `spring-ai-starter-model-openai` por `spring-ai-starter-model-ollama`.
2. Em `worker/src/main/resources/application.yml`, troque o bloco `spring.ai.openai` por:
```yaml
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
```

## Fluxo
React → `POST /api/perguntas` (JWT) → Gateway publica em `perguntas.exchange` → `perguntas.queue`
→ Worker consome, monta o prompt, chama a LLM (3 tentativas, 2 s entre elas) → publica em `respostas.exchange`
→ `respostas.queue` → `@RabbitListener` no Gateway → `SseEmitter` do usuário → React atualiza a tela.
