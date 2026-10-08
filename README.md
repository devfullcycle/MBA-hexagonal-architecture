# MBA Full Cycle — Arquitetura Hexagonal e Clean Architecture

Plataforma de ingressos em que parceiros criam eventos e clientes se inscrevem, gerando ingressos de forma assíncrona. Projeto multi-módulo em Java 17 com Gradle, dividido em `domain`, `application` e `infrastructure`.

## Requisitos

- JDK 17
- Docker e Docker Compose (para o MySQL)

## Como subir o projeto

1. Suba o banco:

   ```bash
   docker compose up -d
   ```

2. Rode a aplicação:

   ```bash
   ./gradlew :infrastructure:bootRun
   ```

   No Windows (PowerShell), use `.\gradlew.bat` em lugar de `./gradlew`.

A API REST sobe em `http://localhost:8080` e o GraphiQL fica em `http://localhost:8080/graphiql`.

## Como rodar a suíte de testes

A suíte completa (domínio, casos de uso e integração) usa H2 em memória no perfil `test` e não precisa do Docker:

```bash
./gradlew test
```

Por módulo:

```bash
./gradlew :domain:test            # regras de negócio dos agregados
./gradlew :application:test       # casos de uso com repositórios in-memory
./gradlew :infrastructure:test    # integração com persistência, REST, GraphQL e fila
```

Relatório de cobertura:

```bash
./gradlew testCodeCoverageReport
```

## Cancelamento de evento

Um parceiro cancela um evento por `POST /events/{id}/cancel` (ou pela mutation `cancelEvent`). O evento passa a `CANCELLED`, deixa de aceitar inscrições, e todos os ingressos daquele evento são cancelados em consequência — de forma desacoplada.

### Onde a cascata acontece

O agregado `Event` nunca toca o agregado `Ticket`. Quem liga os dois é um evento de domínio:

`Event.cancel()` ([Event.java](domain/src/main/java/br/com/fullcycle/domain/event/Event.java)) registra o evento de domínio [`EventCancelled`](domain/src/main/java/br/com/fullcycle/domain/event/EventCancelled.java), de tipo `event.cancelled`, em `allDomainEvents()`. Ao persistir o agregado, [`EventDatabaseRepository`](infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/EventDatabaseRepository.java) grava esse evento na tabela `outbox`. O job [`OutboxRelay`](infrastructure/src/main/java/br/com/fullcycle/infrastructure/job/OutboxRelay.java) publica o conteúdo na fila via `QueueGateway`. O [`ConsumerQueueGateway`](infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java) roteia por tipo e dispara [`CancelEventTicketsUseCase`](application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java), que busca os ingressos por `TicketRepository.ticketsByEventId` e chama `Ticket.cancel()` em cada um. O cancelamento do ingresso é idempotente, então reprocessar a mensagem não quebra o fluxo.

É exatamente o mesmo caminho que o fluxo `event-ticket.reserved` → `CreateTicketForCustomerUseCase` já usava para criar ingressos na inscrição.

O teste que prova esse caminho de ponta a ponta é [`CancelEventTicketsQueueIT`](infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/CancelEventTicketsQueueIT.java).

## Endpoints da feature

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/events/{id}/cancel` | Cancela o evento. `422` com `Event already cancelled` se já estiver cancelado, `422` com `Event not found` se não existir |
| `GET` | `/events/{id}` | Representação completa do evento (`id`, `name`, `date`, `totalSpots`, `status`). `404` se não existir |
| `GET` | `/events/{id}` + header `X-Public: true` | Representação pública reduzida (`id`, `status`). `404` se não existir |

Em GraphQL, a mutation `cancelEvent(id: ID!)` e a query `eventOfId(id: ID!)`.
