# MBA Hexagonal Architecture - Cancelamento de Evento

Projeto multi-modulo em **Java 17** e **Gradle** com **Hexagonal Architecture** e **Clean Architecture**.  
Esta branch evolui o projeto do curso com a feature de **cancelamento de evento** e a cascata assíncrona que cancela os ingressos relacionados.

## Visão geral

O fluxo do sistema é este:

1. Parceiros criam eventos.
2. Clientes se inscrevem nesses eventos.
3. Quando um parceiro cancela um evento, o sistema bloqueia novas inscrições.
4. O cancelamento dos ingressos acontece de forma assíncrona, reagindo a um evento de domínio.

Isso mantém `Event` e `Ticket` desacoplados e preserva a fronteira entre agregado, caso de uso e infraestrutura.

## Arquitetura

O projeto está organizado em três módulos principais:

- `domain` — entidades, value objects, eventos de domínio e regras de negócio.
- `application` — casos de uso, inputs/outputs e contratos.
- `infrastructure` — REST, GraphQL, persistência, fila, outbox e configurações.

O cancelamento segue este caminho:

1. `CancelEventUseCase` muda o status do evento para `CANCELLED`.
2. O agregado publica `EventCancelled`.
3. O `EventDatabaseRepository` persiste o evento na outbox.
4. O `OutboxRelay` envia a mensagem para a fila.
5. O `ConsumerQueueGateway` roteia `event.cancelled`.
6. `CancelEventTicketsUseCase` cancela todos os ingressos do evento.

## Como executar

### Rodar os testes

```bash
docker compose run --rm gradle test
```

### Subir a aplicação

```bash
docker compose up app
```

A API REST fica em `http://localhost:8080`.  
O GraphiQL fica em `http://localhost:8080/graphiql`.

## Endpoints REST

- `POST /events` — cria um evento
- `POST /events/{id}/subscribe` — inscreve um cliente
- `POST /events/{id}/cancel` — cancela um evento
- `GET /events/{id}` — consulta um evento

O `GET /events/{id}` suporta a versão pública via header `X-Public`.

## GraphQL

O módulo de infraestrutura também expõe a feature via GraphQL:

- `createEvent`
- `subscribeCustomerToEvent`
- `cancelEvent`
- `eventOfId`

## Testes manuais

A pasta `tests-http/` contém exemplos de chamadas HTTP para validar o fluxo completo:

- criação de parceiros e eventos
- cancelamento de eventos
- consulta pública e privada de eventos
- bloqueio de novas inscrições após cancelamento
- fluxo completo via GraphQL: criação, consulta e cancelamento
