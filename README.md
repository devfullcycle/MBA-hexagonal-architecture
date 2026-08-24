# MBA Hexagonal Architecture — Plataforma de Ingressos

Projeto do curso de Arquitetura Hexagonal & Clean Architecture (Full Cycle), estendido com a feature de **cancelamento de evento**.

## Como subir o projeto

Pré-requisitos: Java 17 e Docker (para o MySQL).

1. Suba o banco de dados:
   ```bash
   docker-compose up -d
   ```
2. Rode a aplicação:
   ```bash
   ./gradlew :infrastructure:bootRun
   ```
   A API sobe em `http://localhost:8080`. O endpoint REST fica na raiz (`/events`, `/customers`, `/partners`) e o GraphQL em `/graphql` (GraphiQL habilitado em `/graphiql`).

## Como rodar a suíte de testes

```bash
./gradlew test
```

Isso roda os testes de domínio (`domain`), casos de uso com repositórios in-memory (`application`) e os testes de integração/REST (`infrastructure`), estes últimos usando um banco H2 em memória (perfil `test`), sem depender do MySQL do `docker-compose`.

Para rodar só um módulo: `./gradlew :domain:test`, `./gradlew :application:test` ou `./gradlew :infrastructure:test`.

## Coleção de requisições `.http` (opcional, uso local)

Crie localmente uma pasta `requests/` com arquivos `.http` (extensão [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) do VS Code) para testar a API manualmente — um arquivo por entidade (`partners.http`, `customers.http`, `events.http`), com exemplos de criação/cancelamento/consulta de evento. Essa pasta está no `.gitignore` por ser uma conveniência de desenvolvimento, não um entregável do desafio.

## Onde acontece a cascata de cancelamento

Quando um parceiro cancela um evento (`CancelEventUseCase`), o agregado `Event` apenas transiciona seu próprio estado para `CANCELLED` e registra o evento de domínio `EventCancelled` (`domain/.../event/EventCancelled.java`) — ele **não** toca o agregado `Ticket` nem chama nenhum caso de uso de ingresso de forma síncrona.

Esse `EventCancelled` trafega pelo mesmo mecanismo já usado para `EventTicketReserved`: ao persistir o evento (`EventDatabaseRepository`), os eventos de domínio pendentes são gravados na tabela `outbox`; o job `OutboxRelay` publica periodicamente os registros não publicados na fila via `QueueGateway`; e o `ConsumerQueueGateway` roteia a mensagem pelo seu `type` (`event.cancelled`) para o caso de uso `CancelEventTicketsUseCase`, que busca todos os ingressos do evento (`TicketRepository.ticketsByEventId`) e cancela cada um (`Ticket.cancel()`, idempotente).

Esse fluxo assíncrono ponta a ponta — publicação no `ConsumerQueueGateway` até os tickets ficarem `CANCELLED` — é coberto pelo teste `infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGatewayCancelEventIT.java`.
