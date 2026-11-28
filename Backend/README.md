# Dev. Web — Perguntas e Respostas com IA (Gateway + RabbitMQ + Worker + React)

```
Pagina-web-conectada-com-LLM/
├── Backend/
│   ├── docker-compose.yml  RabbitMQ
│   ├── gateway/            Spring Boot: JWT, REST, SSE
│   └── worker/             Spring Boot sem servidor web: Spring AI + Groq
└── Frontend/               React (Vite)
```

## Pré-requisitos
- JDK 17+, Maven 3.9+, Node.js 20+ e Docker com Compose.
- Uma chave da [Groq](https://console.groq.com/keys). A chave deve existir apenas no ambiente do Worker. Não a coloque no React, no YAML ou no Git.

## Como rodar
Execute a partir da raiz do repositório. Cada comando de execução de serviço deve ficar em um terminal separado.

### Linux/macOS (Bash)
```bash
# Terminal 1: RabbitMQ
docker compose -f Backend/docker-compose.yml up -d
# Terminal 2: Gateway
mvn -f Backend/gateway/pom.xml spring-boot:run
# Terminal 3: Worker (a chave não aparece no histórico nem na tela)
read -rsp 'Groq API key: ' GROQ_API_KEY; echo
export GROQ_API_KEY
mvn -f Backend/worker/pom.xml spring-boot:run
# Terminal 4: Frontend
cd Frontend
npm ci
npm run dev
```

### Windows (PowerShell)
```powershell
# Terminal 1
docker compose -f Backend/docker-compose.yml up -d
# Terminal 2
mvn -f Backend/gateway/pom.xml spring-boot:run
# Terminal 3
$env:GROQ_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Groq API key' -AsSecureString)).Password
mvn -f Backend/worker/pom.xml spring-boot:run
# Terminal 4
cd Frontend
npm ci
npm run dev
```

Abra http://localhost:5173, crie uma conta e pergunte. Gateway: http://localhost:8080. Painel RabbitMQ: http://localhost:15672 (admin/admin, ambiente local).

## Modelo e limites
O padrão é `openai/gpt-oss-120b`, executado na Groq, sem necessidade de uma chave ou conta da API da OpenAI. O nome `spring.ai.openai` e o starter correspondente são mantidos por compatibilidade de protocolo com Spring AI 1.0.0. O endereço real é `https://api.groq.com/openai/v1/chat/completions`.

A temperatura 0,4 e o prompt de tutor acadêmico são preservados. O esforço de raciocínio é médio e o limite padrão de conclusão é 4096 tokens (inclui raciocínio). É possível ajustar `GROQ_MAX_COMPLETION_TOKENS` ou selecionar outro modelo GPT-OSS disponível usando `GROQ_MODEL`.

A [cota gratuita](https://console.groq.com/docs/rate-limits) tem limites de requisições e tokens por organização. Confira os limites efetivos da conta. Não há fallback automático para provedores pagos. O `LlmService` permanece responsável por até 3 tentativas, com intervalo de 2 segundos; o retry interno do Spring AI é limitado a uma tentativa para não multiplicar chamadas.

## Validação da migração
1. Execute `mvn -f Backend/worker/pom.xml clean verify`.
2. Suba os quatro componentes e confirme que a resposta retorna pelo SSE, sem `OPENAI_API_KEY`.
3. Pergunte sobre complexidade de busca binária, deadlock, SQL JOIN e uma implementação Java. Confira exatidão, clareza e exemplos em português. A equivalência de qualidade entre modelos exige avaliação das respostas reais; não é garantida pela troca de configuração.
4. Em ambiente de teste, valide a mensagem de erro existente quando a chave for inválida ou a cota estiver esgotada.

## Fluxo
React → `POST /api/perguntas` (JWT) → Gateway publica em `perguntas.exchange` → `perguntas.queue`
→ Worker consome, monta o prompt, chama a Groq → publica em `respostas.topic.exchange`
→ `respostas.queue` → `@RabbitListener` no Gateway → `SseEmitter` do usuário → React atualiza a tela.

## Resposta e stream por tarefa

1. O Gateway registra o dono da tarefa antes de publicar a pergunta e devolve o correlationId no HTTP 202.
2. O React insere a pergunta e abre GET /api/perguntas/{id}/stream com o mesmo JWT. O Gateway devolve 404 para uma tarefa desconhecida, expirada ou de outro usuário.
3. O Worker publica sucesso ou erro em respostas.topic.exchange com a chave `resposta.<correlationId>`. O binding resposta.* encaminha os resultados à fila durável respostas.queue.
4. O Gateway envia o evento resposta e conclui apenas o emitter daquela tarefa. Se o resultado chegar antes da inscrição ou a conexão cair, a mesma URL permite recuperar o resultado. O React tenta novamente em caso de queda antes do evento final.

O registro de tarefas fica na memória de uma única instância do Gateway. Uma tarefa pode aguardar até 30 minutos, e o resultado fica disponível por mais 30 minutos após a conclusão. Um reinício do Gateway encerra essa janela de recuperação.

Para atualizar uma instalação anterior, deixe as perguntas em andamento terminarem antes de reiniciar Gateway, Worker e frontend juntos. A nova exchange topic tem nome próprio para coexistir com a antiga direct. A fila e o volume existentes são reutilizados.
