# Desafio: Cancelamento de Evento com Arquitetura Hexagonal & Clean



## MBA Hexagonal Architecture: Cancelamento de Evento

Projeto multi-módulo (Java 17 / Gradle) organizado em Arquitetura Hexagonal e Clean Architecture, com as camadas `domain`, `application` e `infrastructure` segregadas. Esta branch estende o projeto do curso com a feature de **cancelamento de evento**, incluindo a cascata assíncrona de cancelamento de ingressos.

### Como subir o projeto

O ambiente não depende de Java instalado localmente (tudo roda via Docker).

#### Rodar a suíte de testes

```bash
docker compose run --rm gradle test
```

Isso sobe um container com Gradle 8.2.1 + JDK 17 (`docker compose run --rm gradle <task>` funciona para qualquer task, ex. `build`), monta o repositório em `/workspace` e usa um volume nomeado (`gradle-cache`) para cachear as dependências entre execuções. O MySQL do `docker-compose.yml` sobe como dependência do serviço `gradle`, mas os testes de integração usam H2 em memória (`infrastructure/src/main/resources/application-test.properties`), então o MySQL não é necessário para os testes.

### Subir a aplicação

```bash
docker compose up app
```

Esse serviço (`app`, dedicado ao `bootRun`) já declara a porta 8080 no `docker-compose.yml`, e como estamos usando `docker compose up` (não `run`), a porta é publicada automaticamente no host. Ele também depende do `mysql` com healthcheck, então o banco sobe e fica pronto antes da aplicação tentar conectar. Para encerrar, `Ctrl+C` ou `docker compose down` em outro terminal.

A API REST fica disponível em `http://localhost:8080` e o GraphiQL em `http://localhost:8080/graphiql`.

## Onde a cascata de cancelamento acontece

Quando um parceiro cancela um evento (`CancelEventUseCase`), o agregado `Event` transita para o estado `CANCELLED` e registra o evento de domínio `EventCancelled` (`domain/.../event/EventCancelled.java`, `type = "event.cancelled"`). Esse evento é persistido na tabela `outbox` pelo `EventDatabaseRepository` — o `CancelEventUseCase` não toca no agregado `Ticket` em nenhum momento, síncrono ou não.

O `OutboxRelay` publica periodicamente os eventos pendentes da outbox na fila (`QueueGateway`). O `ConsumerQueueGateway` roteia mensagens cujo conteúdo contém `"event.cancelled"` para o `CancelEventTicketsUseCase` (`application/.../ticket/CancelEventTicketsUseCase.java`), que busca todos os ingressos do evento (`TicketRepository.ticketsByEventId`) e cancela cada um (`Ticket.cancel()`, idempotente). É exatamente o mesmo molde do fluxo `EventTicketReserved → CreateTicketForCustomerUseCase` já existente no projeto, só que reagindo a um evento diferente — os agregados `Event` e `Ticket` seguem desacoplados, comunicando-se somente por eventos de domínio na fila.
