# Cancelamento de Evento — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar a feature de cancelamento de evento: o comando síncrono que cancela o evento e emite o evento de domínio `EventCancelled`, a cascata assíncrona que cancela os ingressos reagindo a esse evento pela outbox + fila, a consulta de evento por id via presenter, e a exposição por REST e GraphQL.

**Architecture:** O agregado `Event` ganha estado (`EventStatus.ACTIVE` / `CANCELLED`) e o método de domínio `cancel()`, que registra `EventCancelled` em `allDomainEvents()`. `EventDatabaseRepository.save` já grava os eventos do agregado na outbox; o `OutboxRelay` já publica na fila via `QueueGateway`; o `ConsumerQueueGateway` ganha uma rota para o tipo `event.cancelled` que dispara `CancelEventTicketsUseCase`, espelhando exatamente o fluxo `event-ticket.reserved` → `CreateTicketForCustomerUseCase` que já existe. O agregado `Event` nunca toca o agregado `Ticket`.

**Tech Stack:** Java 17, Gradle multi-módulo (`domain`, `application`, `infrastructure`), Spring Boot 3.1.2 (Web, Data JPA, GraphQL), JUnit 5, H2 (perfil `test`), MySQL (runtime).

**Spec:** Enunciado do desafio, condensado em "Contexto e Regras" abaixo. O plano argumenta a partir dessa seção — leia-a antes de qualquer task.

---

## Contexto e Regras (spec condensada)

### Comportamento esperado

Um parceiro solicita o cancelamento de um evento pelo id. A partir daí:

1. O evento passa para o estado cancelado e deixa de aceitar novas inscrições.
2. O cancelamento emite o evento de domínio `EventCancelled`.
3. Reagindo a esse evento de domínio, de forma assíncrona pela fila, todos os ingressos daquele evento são cancelados.
4. É possível consultar o evento por id e ver seu estado atual.

### Regras de negócio e mensagens de erro (strings exatas)

| Situação | Resultado |
|---|---|
| Cancelar evento existente e ativo | Evento passa a `CANCELLED` e emite `EventCancelled` |
| Cancelar evento que não existe | `ValidationException("Event not found")` |
| Cancelar evento já cancelado | `ValidationException("Event already cancelled")` |
| Inscrever cliente em evento cancelado | `ValidationException("Event is cancelled")` |
| Cascata de cancelamento dos ingressos | Todos os ingressos do evento passam a `CANCELLED`; reprocessar não quebra (idempotente) |
| Consultar evento por id inexistente | HTTP 404 sem corpo de erro, em **ambos** os presenters |

### Contratos REST

```
POST /events/{id}/cancel

200 OK
{ "id": "f3b6...", "status": "CANCELLED" }

422 Unprocessable Entity
Event already cancelled
```

```
GET /events/{id}                      -> representacao completa
GET /events/{id}  (X-Public: true)    -> representacao publica reduzida

200 OK (completa)
{ "id": "f3b6...", "name": "Disney on Ice", "date": "2021-01-01", "totalSpots": 100, "status": "ACTIVE" }

200 OK (publica)
{ "id": "f3b6...", "status": "ACTIVE" }

404 Not Found (evento inexistente, nos dois casos)
```

### Contrato GraphQL

```graphql
type Mutation {
    cancelEvent(id: ID!): CancelEventResult!
}

type Query {
    eventOfId(id: ID!): Event
}

type CancelEventResult {
    id: ID!
    status: String!
}

type Event {
    id: ID!
    name: String!
    date: String!
    totalSpots: Int!
    status: String
}
```

`status` no `type Event` é **opcional** de propósito: o `type Event` é reutilizado pelo retorno de `createEvent`, cujo Output (`CreateEventUseCase.Output`) não tem `status`. Tornar `status` obrigatório quebraria `createEvent` (regressão).

### Fora de escopo

Reembolso/pagamento; autenticação/autorização além do que já existe; notificação real ao cliente; frontend; troca de banco ou fila; corrida com ingressos ainda em trânsito na fila no instante do cancelamento.

---

## Global Constraints

Estas restrições são o critério de aceite arquitetural. Quebrar qualquer uma descaracteriza a entrega.

- **`domain` não usa framework.** Nenhuma anotação de Spring ou JPA em entidades de domínio, objetos de valor ou eventos de domínio. O módulo `domain` não tem Spring no classpath — qualquer import de `org.springframework` ou `jakarta.persistence` ali é erro de compilação.
- **`application` não conhece infraestrutura.** Casos de uso dependem só de portas (`EventRepository`, `TicketRepository`), nunca de implementações concretas, e têm `Input`/`Output` próprios (records aninhados). O caso de uso não retorna entidade, agregado nem objeto de valor.
- **A cascata não acopla agregados.** `Event` não chama `Ticket`. `CancelEventUseCase` **não** cancela ingressos de forma síncrona. O cancelamento dos ingressos só acontece reagindo ao `EventCancelled` que trafega pela outbox e pela fila.
- **Persistência com mapeamento explícito.** Entidade de domínio separada da entidade JPA (`Event` ↔ `EventEntity`, `Ticket` ↔ `TicketEntity`), convertidas por `of()` / `toEvent()` / `toTicket()`.
- **A saída da consulta passa por presenter.** `GetEventByIdUseCase` não decide formato; o controller escolhe o presenter e chama `useCase.execute(input, presenter)`.
- **O `type` do evento de domínio é exatamente `event.cancelled`.** Não pode colidir com substring dos tipos já roteados (`event-ticket.reserved`, `ticket.created`). Verificado: `"event.cancelled"` não é substring de nenhum deles, e nenhum deles é substring dela.
- **Seguir os padrões existentes:** nomenclatura, organização por contexto, gateways, outbox, `Objects.requireNonNull` nos construtores, testes em `// given` / `// when` / `// then` com `@DisplayName` em português.
- **Regressão:** a suíte existente continua passando. Nada do que já funcionava pode quebrar.

### Fato sobre o ambiente que o plano depende

`ConsumerQueueGateway.publish` é anotado com `@Async(value = "queueExecutor")`, **mas não existe `@EnableAsync` em nenhum lugar do projeto** (`Main` tem apenas `@EnableScheduling`). Logo `@Async` é inerte e `publish()` executa de forma **síncrona**. Por isso o teste ponta-a-ponta da Task 6 pode publicar na fila e assertar imediatamente, sem espera nem `Awaitility`. Não adicione `@EnableAsync`.

### Comandos de teste

Executados da raiz do repositório. No PowerShell use `.\gradlew.bat` em lugar de `./gradlew`.

```bash
./gradlew :domain:test                 # testes de domínio
./gradlew :application:test            # testes de caso de uso
./gradlew :infrastructure:test         # testes de integração
./gradlew test                         # suíte completa
```

Para um teste só: `./gradlew :domain:test --tests "br.com.fullcycle.domain.event.EventTest"`

---

## File Structure

### `domain`

| Arquivo | Responsabilidade |
|---|---|
| Criar `domain/src/main/java/br/com/fullcycle/domain/event/EventStatus.java` | Enum `ACTIVE, CANCELLED` — o estado do agregado `Event` |
| Criar `domain/src/main/java/br/com/fullcycle/domain/event/EventCancelled.java` | Record `DomainEvent` com `type = "event.cancelled"` |
| Modificar `domain/src/main/java/br/com/fullcycle/domain/event/Event.java` | Campo `status`, `cancel()`, `isCancelled()`, `status()`, guarda em `reserveTicket`, `status` no construtor e no `restore` |
| Modificar `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketStatus.java` | Adicionar `CANCELLED` |
| Modificar `domain/src/main/java/br/com/fullcycle/domain/event/ticket/Ticket.java` | `cancel()` idempotente |
| Modificar `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketRepository.java` | Porta `ticketsByEventId(EventId)` |

### `application`

| Arquivo | Responsabilidade |
|---|---|
| Criar `application/src/main/java/br/com/fullcycle/application/event/CancelEventUseCase.java` | Comando: carrega o evento, chama `cancel()`, persiste |
| Criar `application/src/main/java/br/com/fullcycle/application/event/GetEventByIdUseCase.java` | Consulta: `Optional<Output>` com o estado atual |
| Criar `application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java` | Cascata: cancela os ingressos de um evento |
| Modificar `application/src/test/java/br/com/fullcycle/application/repository/InMemoryTicketRepository.java` | Implementar `ticketsByEventId` |

### `infrastructure`

| Arquivo | Responsabilidade |
|---|---|
| Modificar `infrastructure/.../jpa/entities/EventEntity.java` | Coluna `status` e mapeamento nos dois sentidos |
| Modificar `infrastructure/.../jpa/repositories/TicketJpaRepository.java` | `findByEventId(UUID)` |
| Modificar `infrastructure/.../repositories/TicketDatabaseRepository.java` | Implementar `ticketsByEventId` |
| Modificar `infrastructure/.../gateways/ConsumerQueueGateway.java` | Rotear `event.cancelled` → `CancelEventTicketsUseCase` |
| Modificar `infrastructure/.../configurations/UseCaseConfig.java` | Beans dos três novos casos de uso |
| Criar `infrastructure/.../rest/presenters/GetEventByIdResponseEntity.java` | Presenter completo (bean `privateGetEvent`) |
| Criar `infrastructure/.../rest/presenters/PublicGetEventByIdJson.java` | Presenter público reduzido (bean `publicGetEvent`) |
| Modificar `infrastructure/.../rest/EventController.java` | `POST /events/{id}/cancel` e `GET /events/{id}` |
| Modificar `infrastructure/.../graphql/EventResolver.java` | `cancelEvent` e `eventOfId` |
| Modificar `infrastructure/src/main/resources/graphql/schema.gqls` | `cancelEvent`, `eventOfId`, `CancelEventResult`, `status` em `Event` |

### Testes

| Arquivo | Responsabilidade |
|---|---|
| Modificar `domain/src/test/java/br/com/fullcycle/domain/event/EventTest.java` | `cancel()`, cancelar duas vezes, reservar em evento cancelado |
| Modificar `domain/src/test/java/br/com/fullcycle/domain/event/ticket/TicketTest.java` | `cancel()` e idempotência |
| Criar `application/src/test/java/br/com/fullcycle/application/event/CancelEventUseCaseTest.java` | Caminho felizes e de erro com repositórios in-memory |
| Criar `application/src/test/java/br/com/fullcycle/application/event/GetEventByIdUseCaseTest.java` | Consulta encontrada, não encontrada e evento cancelado |
| Criar `application/src/test/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCaseTest.java` | Cascata, idempotência, isolamento entre eventos |
| Criar `infrastructure/src/test/java/br/com/fullcycle/application/usecases/CancelEventUseCaseIT.java` | Persistência real do status |
| Criar `infrastructure/src/test/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepositoryIT.java` | `ticketsByEventId` na persistência real |
| Criar `infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/CancelEventTicketsQueueIT.java` | **O teste central:** fluxo assíncrono ponta-a-ponta |
| Modificar `infrastructure/src/test/java/br/com/fullcycle/infrastructure/rest/EventControllerTest.java` | REST: cancel, get completo, get público, 404 |
| Criar `infrastructure/src/test/java/br/com/fullcycle/infrastructure/graphql/EventResolverTest.java` | GraphQL: mutation e query |

### Documentação

| Arquivo | Responsabilidade |
|---|---|
| Criar `README.md` | Como subir o projeto, como rodar a suíte, onde a cascata acontece |

---

## Task 1: Estado de cancelamento no agregado Event

Muda a assinatura de `Event.restore`, que é chamada por `EventEntity.toEvent()`. Por isso o mapeamento JPA entra nesta mesma task — sem ele o módulo `infrastructure` não compila.

**Files:**
- Create: `domain/src/main/java/br/com/fullcycle/domain/event/EventStatus.java`
- Create: `domain/src/main/java/br/com/fullcycle/domain/event/EventCancelled.java`
- Modify: `domain/src/main/java/br/com/fullcycle/domain/event/Event.java`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/entities/EventEntity.java`
- Test: `domain/src/test/java/br/com/fullcycle/domain/event/EventTest.java`

**Interfaces:**
- Consumes: nada de tasks anteriores.
- Produces:
  - `enum EventStatus { ACTIVE, CANCELLED }` em `br.com.fullcycle.domain.event`
  - `record EventCancelled(String domainEventId, String type, String eventId, Instant occurredOn) implements DomainEvent`, com construtor de conveniência `EventCancelled(EventId eventId)` que fixa `type = "event.cancelled"`
  - `void Event.cancel()` — lança `ValidationException("Event already cancelled")` se já cancelado
  - `EventStatus Event.status()`
  - `boolean Event.isCancelled()`
  - `Event.reserveTicket(CustomerId)` passa a lançar `ValidationException("Event is cancelled")` quando o evento está cancelado
  - Construtor público: `Event(EventId eventId, String name, String date, Integer totalSpots, PartnerId partnerId, String status, Set<EventTicket> tickets)`
  - `static Event restore(String id, String name, String date, int totalSpots, String partnerId, String status, Set<EventTicket> tickets)`

- [ ] **Step 1: Escrever os testes que falham em `EventTest.java`**

Adicione estes três testes ao final da classe `EventTest` (antes do `}` final), mantendo os testes existentes intactos:

```java
    @Test
    @DisplayName("Deve cancelar um evento ativo e registrar o evento de domínio")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var expectedStatus = EventStatus.CANCELLED;
        final var expectedDomainEvent = "event.cancelled";
        final var expectedDomainEvents = 1;

        final var actualEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        Assertions.assertEquals(EventStatus.ACTIVE, actualEvent.status());

        // when
        actualEvent.cancel();

        // then
        Assertions.assertEquals(expectedStatus, actualEvent.status());
        Assertions.assertTrue(actualEvent.isCancelled());
        Assertions.assertEquals(expectedDomainEvents, actualEvent.allDomainEvents().size());

        final var actualDomainEvent = actualEvent.allDomainEvents().iterator().next();
        Assertions.assertEquals(expectedDomainEvent, actualDomainEvent.type());
        Assertions.assertNotNull(actualDomainEvent.domainEventId());
        Assertions.assertNotNull(actualDomainEvent.occurredOn());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelEventTwice() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var expectedError = "Event already cancelled";

        final var actualEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        actualEvent.cancel();

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.cancel()
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve reservar um ticket quando o evento está cancelado")
    public void testReserveTicketWhenEventIsCancelled() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var expectedError = "Event is cancelled";

        final var actualEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        actualEvent.cancel();

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.reserveTicket(aCustomer.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
        Assertions.assertEquals(0, actualEvent.allTickets().size());
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :domain:test --tests "br.com.fullcycle.domain.event.EventTest"`

Expected: FAIL na compilação — `cannot find symbol: class EventStatus`, `cannot find symbol: method cancel()`, `cannot find symbol: method status()`, `cannot find symbol: method isCancelled()`.

- [ ] **Step 3: Criar `EventStatus`**

Arquivo `domain/src/main/java/br/com/fullcycle/domain/event/EventStatus.java`:

```java
package br.com.fullcycle.domain.event;

public enum EventStatus {
    ACTIVE, CANCELLED;
}
```

- [ ] **Step 4: Criar `EventCancelled`**

Arquivo `domain/src/main/java/br/com/fullcycle/domain/event/EventCancelled.java`. Espelha `EventTicketReserved`: record com todos os campos achatados em `String`/`Instant` (para serializar/desserializar como JSON na outbox) mais um construtor de conveniência que fixa o `type`.

```java
package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCancelled(
        String domainEventId,
        String type,
        String eventId,
        Instant occurredOn
) implements DomainEvent {

    public EventCancelled(EventId eventId) {
        this(UUID.randomUUID().toString(), "event.cancelled", eventId.value(), Instant.now());
    }
}
```

- [ ] **Step 5: Evoluir `Event`**

Em `domain/src/main/java/br/com/fullcycle/domain/event/Event.java`, faça cinco edições.

**5a.** Adicione o campo `status` junto aos outros campos mutáveis (depois de `private PartnerId partnerId;`):

```java
    private PartnerId partnerId;
    private EventStatus status;
```

**5b.** Adicione o parâmetro `status` ao construtor público, entre `partnerId` e `tickets`, e a chamada a `setStatus`:

```java
    public Event(
            final EventId eventId,
            final String name,
            final String date,
            final Integer totalSpots,
            final PartnerId partnerId,
            final String status,
            final Set<EventTicket> tickets
    ) {
        this(eventId, tickets);
        this.setName(name);
        this.setDate(date);
        this.setTotalSpots(totalSpots);
        this.setPartnerId(partnerId);
        this.setStatus(status);
    }
```

**5c.** Atualize `newEvent` e `restore`:

```java
    public static Event newEvent(final String name, final String date, final Integer totalSpots, final Partner partner) {
        return new Event(EventId.unique(), name, date, totalSpots, partner.partnerId(), EventStatus.ACTIVE.name(), null);
    }

    public static Event restore(
            final String id,
            final String name,
            final String date,
            final int totalSpots,
            final String partnerId,
            final String status,
            final Set<EventTicket> tickets
    ) {
        return new Event(EventId.with(id), name, date, totalSpots, PartnerId.with(partnerId), status, tickets);
    }
```

**5d.** Adicione a guarda no início de `reserveTicket`, antes da checagem de cliente duplicado — a regra de evento cancelado é mais forte que as outras. Método completo depois da edição:

```java
    public EventTicket reserveTicket(final CustomerId aCustomerId) {
        if (isCancelled()) {
            throw new ValidationException("Event is cancelled");
        }

        this.allTickets().stream()
                .filter(it -> Objects.equals(it.customerId(), aCustomerId))
                .findFirst()
                .ifPresent(it -> {
                    throw new ValidationException("Email already registered");
                });

        if (totalSpots() < allTickets().size() + ONE) {
            throw new ValidationException("Event sold out");
        }

        final var aTicket =
                EventTicket.newTicket(eventId(), aCustomerId, allTickets().size() + 1);

        this.tickets.add(aTicket);
        this.domainEvents.add(new EventTicketReserved(aTicket.eventTicketId(), eventId(), aCustomerId));

        return aTicket;
    }
```

**5e.** Adicione `cancel()`, `status()` e `isCancelled()` logo depois de `reserveTicket`, e o `setStatus` privado junto dos outros setters privados no fim da classe:

```java
    public void cancel() {
        if (isCancelled()) {
            throw new ValidationException("Event already cancelled");
        }

        this.status = EventStatus.CANCELLED;
        this.domainEvents.add(new EventCancelled(eventId()));
    }

    public EventStatus status() {
        return status;
    }

    public boolean isCancelled() {
        return EventStatus.CANCELLED == this.status;
    }
```

```java
    private void setStatus(final String status) {
        if (status == null) {
            throw new ValidationException("Invalid status for Event");
        }

        try {
            this.status = EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid status for Event", ex);
        }
    }
```

- [ ] **Step 6: Rodar os testes de domínio e confirmar que passam**

Run: `./gradlew :domain:test`

Expected: PASS — todos os testes de `EventTest` (os 5 antigos e os 3 novos) e o resto do módulo `domain`.

- [ ] **Step 7: Refletir o status na entidade JPA**

`EventEntity` precisa acompanhar a nova assinatura de `Event.restore`. Em `infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/entities/EventEntity.java`:

**7a.** Adicione o import de `EventStatus` junto aos outros imports de domínio:

```java
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.event.EventTicket;
```

**7b.** Adicione o campo persistido depois de `partnerId`:

```java
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    private EventStatus status;
```

**7c.** Adicione o parâmetro ao construtor cheio:

```java
    public EventEntity(UUID id, String name, LocalDate date, int totalSpots, UUID partnerId, EventStatus status) {
        this();
        this.id = id;
        this.name = name;
        this.date = date;
        this.totalSpots = totalSpots;
        this.partnerId = partnerId;
        this.status = status;
    }
```

**7d.** Atualize `of` e `toEvent`. O fallback para `ACTIVE` em `toEvent` existe porque uma base MySQL criada antes desta feature tem linhas em `events` com `status` nulo (`ddl-auto=update` adiciona a coluna sem preencher):

```java
    public static EventEntity of(final Event event) {
        final var entity = new EventEntity(
                UUID.fromString(event.eventId().value()),
                event.name().value(),
                event.date(),
                event.totalSpots(),
                UUID.fromString(event.partnerId().value()),
                event.status()
        );

        event.allTickets().forEach(entity::addTicket);

        return entity;
    }

    public Event toEvent() {
        return Event.restore(
                this.id().toString(),
                this.name(),
                this.date().format(DateTimeFormatter.ISO_LOCAL_DATE),
                this.totalSpots(),
                this.partnerId().toString(),
                this.status() != null ? this.status().name() : EventStatus.ACTIVE.name(),
                this.tickets().stream()
                        .map(EventTicketEntity::toEventTicket)
                        .collect(Collectors.toSet())
        );
    }
```

**7e.** Adicione os acessores, seguindo o estilo da classe (getter sem prefixo, setter com `set`), depois de `setPartnerId`:

```java
    public EventStatus status() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
```

- [ ] **Step 8: Rodar a suíte completa e confirmar que nada quebrou**

Run: `./gradlew test`

Expected: PASS — inclusive `EventControllerTest` e `CreateEventUseCaseIT`, que exercitam `EventEntity` de ponta a ponta.

- [ ] **Step 9: Commit**

```bash
git add domain/src/main/java/br/com/fullcycle/domain/event/EventStatus.java domain/src/main/java/br/com/fullcycle/domain/event/EventCancelled.java domain/src/main/java/br/com/fullcycle/domain/event/Event.java domain/src/test/java/br/com/fullcycle/domain/event/EventTest.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/entities/EventEntity.java
git commit -m "feat: adiciona estado de cancelamento ao agregado Event"
```

---

## Task 2: Cancelamento de Ticket e a porta de busca por evento

A cascata precisa de duas coisas no lado do ingresso: um método de domínio para cancelar e uma forma de achar todos os ingressos de um evento. A porta `TicketRepository` é implementada por dois adapters (in-memory nos testes de caso de uso, banco na infraestrutura) — ambos entram nesta task, senão a compilação quebra.

**Files:**
- Modify: `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketStatus.java`
- Modify: `domain/src/main/java/br/com/fullcycle/domain/event/ticket/Ticket.java`
- Modify: `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketRepository.java`
- Modify: `application/src/test/java/br/com/fullcycle/application/repository/InMemoryTicketRepository.java`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/repositories/TicketJpaRepository.java`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepository.java`
- Test: `domain/src/test/java/br/com/fullcycle/domain/event/ticket/TicketTest.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepositoryIT.java`

**Interfaces:**
- Consumes: nada da Task 1.
- Produces:
  - `TicketStatus.CANCELLED`
  - `void Ticket.cancel()` — idempotente: cancelar um ingresso já cancelado não lança nada
  - `List<Ticket> TicketRepository.ticketsByEventId(EventId anId)` — lista vazia quando o evento não tem ingressos
  - `List<TicketEntity> TicketJpaRepository.findByEventId(UUID eventId)`

- [ ] **Step 1: Escrever os testes de domínio que falham em `TicketTest.java`**

Adicione ao final da classe `TicketTest` (antes do `}` final):

```java
    @Test
    @DisplayName("Deve cancelar um ticket")
    public void testCancelTicket() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var anEvent =
                Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedStatus = TicketStatus.CANCELLED;

        final var actualTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());

        // when
        actualTicket.cancel();

        // then
        Assertions.assertEquals(expectedStatus, actualTicket.status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao cancelar um ticket já cancelado")
    public void testCancelTicketTwice() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var anEvent =
                Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedStatus = TicketStatus.CANCELLED;

        final var actualTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());
        actualTicket.cancel();

        // when
        Assertions.assertDoesNotThrow(() -> actualTicket.cancel());

        // then
        Assertions.assertEquals(expectedStatus, actualTicket.status());
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :domain:test --tests "br.com.fullcycle.domain.event.ticket.TicketTest"`

Expected: FAIL na compilação — `cannot find symbol: variable CANCELLED`, `cannot find symbol: method cancel()`.

- [ ] **Step 3: Adicionar `CANCELLED` ao `TicketStatus`**

Arquivo `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketStatus.java`:

```java
package br.com.fullcycle.domain.event.ticket;

public enum TicketStatus {
    PENDING, PROCESSING, PAID, CANCELLED;
}
```

- [ ] **Step 4: Adicionar `Ticket.cancel()`**

Em `domain/src/main/java/br/com/fullcycle/domain/event/ticket/Ticket.java`, adicione o método depois do segundo `newTicket` e antes do getter `ticketId()`. O `return` antecipado é o que torna a cascata idempotente — reprocessar o `EventCancelled` não lança nada:

```java
    public void cancel() {
        if (TicketStatus.CANCELLED == this.status) {
            return;
        }

        this.setStatus(TicketStatus.CANCELLED);
    }
```

- [ ] **Step 5: Rodar os testes de domínio e confirmar que passam**

Run: `./gradlew :domain:test`

Expected: PASS.

- [ ] **Step 6: Estender a porta `TicketRepository`**

Arquivo `domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketRepository.java` — arquivo completo:

```java
package br.com.fullcycle.domain.event.ticket;

import br.com.fullcycle.domain.event.EventId;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> ticketOfId(TicketId anId);

    List<Ticket> ticketsByEventId(EventId anId);

    Ticket create(Ticket ticket);

    Ticket update(Ticket ticket);

    void deleteAll();
}
```

- [ ] **Step 7: Implementar no adapter in-memory**

Arquivo `application/src/test/java/br/com/fullcycle/application/repository/InMemoryTicketRepository.java` — arquivo completo:

```java
package br.com.fullcycle.application.repository;

import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> tickets;

    public InMemoryTicketRepository() {
        this.tickets = new HashMap<>();
    }

    @Override
    public Optional<Ticket> ticketOfId(TicketId anId) {
        return Optional.ofNullable(this.tickets.get(Objects.requireNonNull(anId).value()));
    }

    @Override
    public List<Ticket> ticketsByEventId(EventId anId) {
        final var eventId = Objects.requireNonNull(anId).value();
        return this.tickets.values().stream()
                .filter(it -> Objects.equals(eventId, it.eventId().value()))
                .toList();
    }

    @Override
    public Ticket create(Ticket ticket) {
        this.tickets.put(ticket.ticketId().value(), ticket);
        return ticket;
    }

    @Override
    public Ticket update(Ticket ticket) {
        this.tickets.put(ticket.ticketId().value(), ticket);
        return ticket;
    }

    @Override
    public void deleteAll() {
        this.tickets.clear();
    }
}
```

- [ ] **Step 8: Escrever o teste de integração que falha**

Arquivo novo `infrastructure/src/test/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepositoryIT.java`:

```java
package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar somente os tickets do evento informado")
    public void testTicketsByEventId() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var janeDoe =
                customerRepository.create(Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var anotherEvent =
                eventRepository.create(Event.newEvent("Frozen on Ice", "2021-02-01", 10, aPartner));

        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(janeDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anotherEvent.eventId()));

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());

        // then
        Assertions.assertEquals(2, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> anEvent.eventId().equals(it.eventId())));
        Assertions.assertEquals(1, ticketRepository.ticketsByEventId(anotherEvent.eventId()).size());
    }

    @Test
    @DisplayName("Deve persistir o status CANCELLED de um ticket")
    public void testUpdateCancelledTicket() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var aTicket =
                ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));

        Assertions.assertEquals(TicketStatus.PENDING, aTicket.status());

        // when
        aTicket.cancel();
        ticketRepository.update(aTicket);

        // then
        final var actualTicket = ticketRepository.ticketOfId(aTicket.ticketId()).get();
        Assertions.assertEquals(TicketStatus.CANCELLED, actualTicket.status());
    }
}
```

- [ ] **Step 9: Rodar o teste de integração e confirmar que falha**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.repositories.TicketDatabaseRepositoryIT"`

Expected: FAIL na compilação de `infrastructure` — `TicketDatabaseRepository is not abstract and does not override abstract method ticketsByEventId(EventId)`.

- [ ] **Step 10: Adicionar a query JPA**

Arquivo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/repositories/TicketJpaRepository.java` — arquivo completo. O nome do método é derivado pelo Spring Data a partir do campo `eventId` de `TicketEntity`, sem precisar de `@Query`:

```java
package br.com.fullcycle.infrastructure.jpa.repositories;

import br.com.fullcycle.infrastructure.jpa.entities.TicketEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface TicketJpaRepository extends CrudRepository<TicketEntity, UUID> {

    List<TicketEntity> findByEventId(UUID eventId);
}
```

- [ ] **Step 11: Implementar `ticketsByEventId` no adapter de banco**

Em `infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepository.java`:

**11a.** Adicione os imports que faltam:

```java
import br.com.fullcycle.domain.event.EventId;
```

```java
import java.util.List;
```

**11b.** Adicione o método logo depois de `ticketOfId`:

```java
    @Override
    public List<Ticket> ticketsByEventId(final EventId anId) {
        Objects.requireNonNull(anId, "id cannot be null");
        return this.ticketJpaRepository.findByEventId(UUID.fromString(anId.value()))
                .stream()
                .map(TicketEntity::toTicket)
                .toList();
    }
```

- [ ] **Step 12: Rodar a suíte completa e confirmar que passa**

Run: `./gradlew test`

Expected: PASS, incluindo os dois testes novos de `TicketDatabaseRepositoryIT`.

- [ ] **Step 13: Commit**

```bash
git add domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketStatus.java domain/src/main/java/br/com/fullcycle/domain/event/ticket/Ticket.java domain/src/main/java/br/com/fullcycle/domain/event/ticket/TicketRepository.java domain/src/test/java/br/com/fullcycle/domain/event/ticket/TicketTest.java application/src/test/java/br/com/fullcycle/application/repository/InMemoryTicketRepository.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/jpa/repositories/TicketJpaRepository.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepository.java infrastructure/src/test/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepositoryIT.java
git commit -m "feat: adiciona cancelamento de ticket e busca de tickets por evento"
```

---

## Task 3: CancelEventUseCase

O comando. Carrega o agregado, chama `cancel()`, persiste. **Não** toca em ingresso nenhum — quem faz isso é a Task 4, reagindo ao evento de domínio.

**Files:**
- Create: `application/src/main/java/br/com/fullcycle/application/event/CancelEventUseCase.java`
- Test: `application/src/test/java/br/com/fullcycle/application/event/CancelEventUseCaseTest.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/application/usecases/CancelEventUseCaseIT.java`

**Interfaces:**
- Consumes: `Event.cancel()`, `Event.status()`, `EventStatus` (Task 1).
- Produces:
  - `class CancelEventUseCase extends UseCase<CancelEventUseCase.Input, CancelEventUseCase.Output>`
  - `CancelEventUseCase(EventRepository eventRepository)`
  - `record Input(String eventId)`
  - `record Output(String id, String status)` — `status` é o `name()` do `EventStatus`, ou seja `"CANCELLED"`

- [ ] **Step 1: Escrever os testes que falham**

Arquivo novo `application/src/test/java/br/com/fullcycle/application/event/CancelEventUseCaseTest.java`:

```java
package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventUseCaseTest {

    @Test
    @DisplayName("Deve cancelar um evento")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var eventID = anEvent.eventId().value();
        final var expectedStatus = EventStatus.CANCELLED.name();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var cancelInput = new CancelEventUseCase.Input(eventID);

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(eventID, output.id());
        Assertions.assertEquals(expectedStatus, output.status());

        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertTrue(actualEvent.isCancelled());
    }

    @Test
    @DisplayName("Deve registrar o evento de domínio event.cancelled ao cancelar")
    public void testCancelEventRegistersDomainEvent() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedDomainEvent = "event.cancelled";

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var cancelInput = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        useCase.execute(cancelInput);

        // then
        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(1, actualEvent.allDomainEvents().size());
        Assertions.assertEquals(expectedDomainEvent, actualEvent.allDomainEvents().iterator().next().type());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEventWithoutEvent() throws Exception {
        // given
        final var expectedError = "Event not found";

        final var eventRepository = new InMemoryEventRepository();

        final var cancelInput = new CancelEventUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var actualException =
                Assertions.assertThrows(ValidationException.class, () -> useCase.execute(cancelInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelEventAlreadyCancelled() throws Exception {
        // given
        final var expectedError = "Event already cancelled";

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var cancelInput = new CancelEventUseCase.Input(anEvent.eventId().value());

        final var useCase = new CancelEventUseCase(eventRepository);
        useCase.execute(cancelInput);

        // when
        final var actualException =
                Assertions.assertThrows(ValidationException.class, () -> useCase.execute(cancelInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :application:test --tests "br.com.fullcycle.application.event.CancelEventUseCaseTest"`

Expected: FAIL na compilação — `cannot find symbol: class CancelEventUseCase`.

- [ ] **Step 3: Implementar o caso de uso**

Arquivo novo `application/src/main/java/br/com/fullcycle/application/event/CancelEventUseCase.java`:

```java
package br.com.fullcycle.application.event;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.exceptions.ValidationException;

import java.util.Objects;

public class CancelEventUseCase extends UseCase<CancelEventUseCase.Input, CancelEventUseCase.Output> {

    private final EventRepository eventRepository;

    public CancelEventUseCase(final EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
    }

    @Override
    public Output execute(final Input input) {
        final var anEvent = eventRepository.eventOfId(EventId.with(input.eventId()))
                .orElseThrow(() -> new ValidationException("Event not found"));

        anEvent.cancel();

        eventRepository.update(anEvent);

        return new Output(anEvent.eventId().value(), anEvent.status().name());
    }

    public record Input(String eventId) {
    }

    public record Output(String id, String status) {
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :application:test`

Expected: PASS — os 4 testes novos mais todos os já existentes.

- [ ] **Step 5: Escrever o teste de integração da persistência do status**

Este teste prova que `EventStatus` sobrevive a um round-trip no banco real. Arquivo novo `infrastructure/src/test/java/br/com/fullcycle/application/usecases/CancelEventUseCaseIT.java`:

```java
package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventUseCaseIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase useCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve persistir o evento como cancelado")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var expectedStatus = EventStatus.CANCELLED;

        // when
        final var output = useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.id());
        Assertions.assertEquals(expectedStatus.name(), output.status());

        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(expectedStatus, actualEvent.status());
        Assertions.assertTrue(actualEvent.isCancelled());
    }

    @Test
    @DisplayName("Não deve aceitar inscrição em um evento cancelado")
    public void testSubscribeToCancelledEvent() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var expectedError = "Event is cancelled";

        useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        final var actualEvent = eventRepository.eventOfId(EventId.with(anEvent.eventId().value())).get();

        // when
        final var actualException = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.reserveTicket(johnDoe.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEventWithoutEvent() throws Exception {
        // given
        final var expectedError = "Event not found";

        // when
        final var actualException = Assertions.assertThrows(
                ValidationException.class,
                () -> useCase.execute(new CancelEventUseCase.Input(EventId.unique().value()))
        );

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}
```

- [ ] **Step 6: Rodar o teste de integração e confirmar que falha**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.application.usecases.CancelEventUseCaseIT"`

Expected: FAIL no start do contexto Spring — `NoSuchBeanDefinitionException: No qualifying bean of type 'br.com.fullcycle.application.event.CancelEventUseCase'`. O bean entra na Task 6.

- [ ] **Step 7: Commit**

O IT fica vermelho até a Task 6 registrar o bean, então ele **não** entra neste commit — só o caso de uso e seus testes unitários:

```bash
git add application/src/main/java/br/com/fullcycle/application/event/CancelEventUseCase.java application/src/test/java/br/com/fullcycle/application/event/CancelEventUseCaseTest.java
git commit -m "feat: adiciona CancelEventUseCase"
```

Mantenha `CancelEventUseCaseIT.java` no diretório de trabalho, sem commit. Ele será commitado na Task 6, junto com o bean que o faz passar.

---

## Task 4: CancelEventTicketsUseCase — a cascata

O caso de uso que reage ao `EventCancelled`. Depende só da porta `TicketRepository`: não conhece `Event`, não conhece fila, não conhece Spring.

**Files:**
- Create: `application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java`
- Test: `application/src/test/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCaseTest.java`

**Interfaces:**
- Consumes: `Ticket.cancel()`, `TicketRepository.ticketsByEventId(EventId)` (Task 2).
- Produces:
  - `class CancelEventTicketsUseCase extends UseCase<CancelEventTicketsUseCase.Input, CancelEventTicketsUseCase.Output>`
  - `CancelEventTicketsUseCase(TicketRepository ticketRepository)`
  - `record Input(String eventId)`
  - `record Output(String eventId, int cancelledTickets)` — `cancelledTickets` é a quantidade de ingressos do evento que estão cancelados ao fim da execução, portanto estável entre reprocessamentos

- [ ] **Step 1: Escrever os testes que falham**

Arquivo novo `application/src/test/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCaseTest.java`:

```java
package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var janeDoe = Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com");

        final var expectedCancelledTickets = 2;

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(janeDoe.customerId(), anEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.eventId());
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(expectedCancelledTickets, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> TicketStatus.CANCELLED == it.status()));
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar o cancelamento")
    public void testCancelEventTicketsTwice() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var expectedCancelledTickets = 1;

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // when
        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(cancelInput));

        // then
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> TicketStatus.CANCELLED == it.status()));
    }

    @Test
    @DisplayName("Não deve cancelar tickets de outros eventos")
    public void testCancelEventTicketsDoesNotTouchOtherEvents() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var anotherEvent = Event.newEvent("Frozen on Ice", "2021-02-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anotherEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // then
        final var untouchedTickets = ticketRepository.ticketsByEventId(anotherEvent.eventId());
        Assertions.assertEquals(1, untouchedTickets.size());
        Assertions.assertEquals(TicketStatus.PENDING, untouchedTickets.get(0).status());
    }

    @Test
    @DisplayName("Deve concluir sem erro quando o evento não tem tickets")
    public void testCancelEventTicketsWithoutTickets() throws Exception {
        // given
        final var expectedCancelledTickets = 0;

        final var ticketRepository = new InMemoryTicketRepository();

        final var cancelInput = new CancelEventTicketsUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :application:test --tests "br.com.fullcycle.application.ticket.CancelEventTicketsUseCaseTest"`

Expected: FAIL na compilação — `cannot find symbol: class CancelEventTicketsUseCase`.

- [ ] **Step 3: Implementar o caso de uso**

Arquivo novo `application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java`:

```java
package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;

import java.util.Objects;

public class CancelEventTicketsUseCase
        extends UseCase<CancelEventTicketsUseCase.Input, CancelEventTicketsUseCase.Output> {

    private final TicketRepository ticketRepository;

    public CancelEventTicketsUseCase(final TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
    }

    @Override
    public Output execute(final Input input) {
        final var anEventId = EventId.with(input.eventId());

        final var tickets = this.ticketRepository.ticketsByEventId(anEventId);

        tickets.forEach(it -> {
            it.cancel();
            this.ticketRepository.update(it);
        });

        return new Output(anEventId.value(), tickets.size());
    }

    public record Input(String eventId) {
    }

    public record Output(String eventId, int cancelledTickets) {
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :application:test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java application/src/test/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCaseTest.java
git commit -m "feat: adiciona CancelEventTicketsUseCase para a cascata de cancelamento"
```

---

## Task 5: GetEventByIdUseCase

A consulta. Retorna `Optional<Output>` — o `Optional` vazio é o que os presenters traduzem em 404.

**Files:**
- Create: `application/src/main/java/br/com/fullcycle/application/event/GetEventByIdUseCase.java`
- Test: `application/src/test/java/br/com/fullcycle/application/event/GetEventByIdUseCaseTest.java`

**Interfaces:**
- Consumes: `Event.status()` (Task 1), `Event.cancel()` (Task 1) nos testes.
- Produces:
  - `class GetEventByIdUseCase extends UseCase<GetEventByIdUseCase.Input, Optional<GetEventByIdUseCase.Output>>`
  - `GetEventByIdUseCase(EventRepository eventRepository)`
  - `record Input(String id)`
  - `record Output(String id, String name, String date, int totalSpots, String status)` — `date` em `ISO_LOCAL_DATE` (`2021-01-01`), `status` é o `name()` do `EventStatus`

- [ ] **Step 1: Escrever os testes que falham**

Arquivo novo `application/src/test/java/br/com/fullcycle/application/event/GetEventByIdUseCaseTest.java`:

```java
package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEventByIdUseCaseTest {

    @Test
    @DisplayName("Deve obter um evento ativo por id")
    public void testGetActiveEventById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 100, aPartner);

        final var expectedId = anEvent.eventId().value();
        final var expectedName = "Disney on Ice";
        final var expectedDate = "2021-01-01";
        final var expectedTotalSpots = 100;
        final var expectedStatus = EventStatus.ACTIVE.name();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(expectedId));

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(expectedId, output.get().id());
        Assertions.assertEquals(expectedName, output.get().name());
        Assertions.assertEquals(expectedDate, output.get().date());
        Assertions.assertEquals(expectedTotalSpots, output.get().totalSpots());
        Assertions.assertEquals(expectedStatus, output.get().status());
    }

    @Test
    @DisplayName("Deve obter um evento cancelado com o status CANCELLED")
    public void testGetCancelledEventById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 100, aPartner);
        anEvent.cancel();

        final var expectedStatus = EventStatus.CANCELLED.name();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(expectedStatus, output.get().status());
    }

    @Test
    @DisplayName("Deve retornar vazio ao obter um evento que não existe")
    public void testGetEventByIdWithoutEvent() throws Exception {
        // given
        final var eventRepository = new InMemoryEventRepository();

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(EventId.unique().value()));

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :application:test --tests "br.com.fullcycle.application.event.GetEventByIdUseCaseTest"`

Expected: FAIL na compilação — `cannot find symbol: class GetEventByIdUseCase`.

- [ ] **Step 3: Implementar o caso de uso**

Arquivo novo `application/src/main/java/br/com/fullcycle/application/event/GetEventByIdUseCase.java`:

```java
package br.com.fullcycle.application.event;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class GetEventByIdUseCase
        extends UseCase<GetEventByIdUseCase.Input, Optional<GetEventByIdUseCase.Output>> {

    private final EventRepository eventRepository;

    public GetEventByIdUseCase(final EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
    }

    @Override
    public Optional<Output> execute(final Input input) {
        return eventRepository.eventOfId(EventId.with(input.id))
                .map(e -> new Output(
                        e.eventId().value(),
                        e.name().value(),
                        e.date().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        e.totalSpots(),
                        e.status().name())
                );
    }

    public record Input(String id) {
    }

    public record Output(String id, String name, String date, int totalSpots, String status) {
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :application:test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add application/src/main/java/br/com/fullcycle/application/event/GetEventByIdUseCase.java application/src/test/java/br/com/fullcycle/application/event/GetEventByIdUseCaseTest.java
git commit -m "feat: adiciona GetEventByIdUseCase"
```

---

## Task 6: Composição e o fluxo assíncrono ponta-a-ponta

O coração do desafio. Registra os casos de uso como beans, liga o `event.cancelled` à cascata no `ConsumerQueueGateway`, e prova por asserção que o fluxo real funciona.

**Files:**
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/configurations/UseCaseConfig.java`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/CancelEventTicketsQueueIT.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/application/usecases/CancelEventUseCaseIT.java` (escrito na Task 3, passa a verde aqui)

**Interfaces:**
- Consumes: `CancelEventUseCase` (Task 3), `CancelEventTicketsUseCase` (Task 4), `GetEventByIdUseCase` (Task 5), `EventCancelled` (Task 1), `TicketRepository.ticketsByEventId` (Task 2).
- Produces:
  - Beans Spring `cancelEventUseCase`, `cancelEventTicketsUseCase`, `getEventByIdUseCase`
  - `ConsumerQueueGateway(CreateTicketForCustomerUseCase, CancelEventTicketsUseCase, ObjectMapper)` — nova assinatura do construtor

- [ ] **Step 1: Escrever o teste ponta-a-ponta que falha**

Este é o teste mais importante do desafio: ele dispara o `EventCancelled` pelo caminho real (o JSON publicado no `ConsumerQueueGateway`) e comprova que os ingressos terminam `CANCELLED`, provando o roteamento — não só que o caso de uso funciona isolado.

Dois detalhes do desenho:
- `queueGateway.publish(...)` executa de forma síncrona (não há `@EnableAsync` no projeto), então a asserção vem direto, sem espera.
- O primeiro teste cria os ingressos com `Ticket.newTicket(customerId, eventId)` — a sobrecarga de 2 argumentos, que **não** registra evento de domínio. Assim nada vai para a outbox e o `OutboxRelay` agendado (que roda a cada 2s em background durante os testes) não interfere no resultado.
- O segundo teste cobre a perna da outbox: `CancelEventUseCase` grava o `event.cancelled` na tabela `outbox`, que é de onde o relay publica.

Arquivo novo `infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/CancelEventTicketsQueueIT.java`:

```java
package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

class CancelEventTicketsQueueIT extends IntegrationTest {

    @Autowired
    private QueueGateway queueGateway;

    @Autowired
    private CancelEventUseCase cancelEventUseCase;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        outboxJpaRepository.deleteAll();
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar os tickets do evento ao consumir o EventCancelled da fila")
    public void testCancelEventTicketsWhenEventCancelledIsConsumed() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var janeDoe =
                customerRepository.create(Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var anotherEvent =
                eventRepository.create(Event.newEvent("Frozen on Ice", "2021-02-01", 10, aPartner));

        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(janeDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anotherEvent.eventId()));

        final var content = mapper.writeValueAsString(new EventCancelled(anEvent.eventId()));

        // when
        queueGateway.publish(content);

        // then
        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(2, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> TicketStatus.CANCELLED == it.status()));

        final var untouchedTickets = ticketRepository.ticketsByEventId(anotherEvent.eventId());
        Assertions.assertEquals(1, untouchedTickets.size());
        Assertions.assertEquals(TicketStatus.PENDING, untouchedTickets.get(0).status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao reconsumir o EventCancelled da fila")
    public void testCancelEventTicketsIsIdempotent() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));

        final var content = mapper.writeValueAsString(new EventCancelled(anEvent.eventId()));

        queueGateway.publish(content);

        // when
        Assertions.assertDoesNotThrow(() -> queueGateway.publish(content));

        // then
        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(1, actualTickets.size());
        Assertions.assertEquals(TicketStatus.CANCELLED, actualTickets.get(0).status());
    }

    @Test
    @DisplayName("Deve gravar o EventCancelled na outbox ao cancelar o evento")
    public void testEventCancelledIsStoredInOutbox() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        // when
        cancelEventUseCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        final List<OutboxEntity> outbox = new ArrayList<>();
        outboxJpaRepository.findAll().forEach(outbox::add);

        Assertions.assertTrue(
                outbox.stream().anyMatch(it -> it.content().contains("event.cancelled")),
                "a outbox deveria conter o evento de dominio event.cancelled"
        );
        Assertions.assertTrue(
                outbox.stream().anyMatch(it -> it.content().contains(anEvent.eventId().value())),
                "a outbox deveria conter o id do evento cancelado"
        );
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.gateways.CancelEventTicketsQueueIT"`

Expected: FAIL no start do contexto Spring — `NoSuchBeanDefinitionException: No qualifying bean of type 'br.com.fullcycle.application.event.CancelEventUseCase'`.

- [ ] **Step 3: Registrar os três casos de uso como beans**

Em `infrastructure/src/main/java/br/com/fullcycle/infrastructure/configurations/UseCaseConfig.java`:

**3a.** Adicione os imports:

```java
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.application.ticket.CancelEventTicketsUseCase;
```

**3b.** Adicione os três beans no fim da classe, antes do `}` final:

```java
    @Bean
    public CancelEventUseCase cancelEventUseCase() {
        return new CancelEventUseCase(eventRepository);
    }

    @Bean
    public GetEventByIdUseCase getEventByIdUseCase() {
        return new GetEventByIdUseCase(eventRepository);
    }

    @Bean
    public CancelEventTicketsUseCase cancelEventTicketsUseCase() {
        return new CancelEventTicketsUseCase(ticketRepository);
    }
```

- [ ] **Step 4: Rotear `event.cancelled` no `ConsumerQueueGateway`**

Arquivo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java` — arquivo completo. A nova rota espelha a existente: checa o tipo no conteúdo, desserializa o record do evento de domínio, dispara o caso de uso com o `Input` próprio dele.

```java
package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.application.ticket.CancelEventTicketsUseCase;
import br.com.fullcycle.application.ticket.CreateTicketForCustomerUseCase;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventTicketReserved;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ConsumerQueueGateway implements QueueGateway {

    private final CreateTicketForCustomerUseCase createTicketForCustomerUseCase;
    private final CancelEventTicketsUseCase cancelEventTicketsUseCase;
    private final ObjectMapper mapper;

    public ConsumerQueueGateway(
            final CreateTicketForCustomerUseCase createTicketForCustomerUseCase,
            final CancelEventTicketsUseCase cancelEventTicketsUseCase,
            final ObjectMapper mapper
    ) {
        this.createTicketForCustomerUseCase = Objects.requireNonNull(createTicketForCustomerUseCase);
        this.cancelEventTicketsUseCase = Objects.requireNonNull(cancelEventTicketsUseCase);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Async(value = "queueExecutor")
    @Override
    public void publish(final String content) {
        if (content == null) {
            return;
        }

        if (content.contains("event-ticket.reserved")) {
            final var dto = safeRead(content, EventTicketReserved.class);
            this.createTicketForCustomerUseCase.execute(new CreateTicketForCustomerUseCase.Input(dto.eventTicketId(), dto.eventId(), dto.customerId()));
        }

        if (content.contains("event.cancelled")) {
            final var dto = safeRead(content, EventCancelled.class);
            this.cancelEventTicketsUseCase.execute(new CancelEventTicketsUseCase.Input(dto.eventId()));
        }
    }

    private <T> T safeRead(final String content, final Class<T> tClass) {
        try {
            return this.mapper.readValue(content, tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 5: Rodar os testes de integração e confirmar que passam**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.gateways.CancelEventTicketsQueueIT" --tests "br.com.fullcycle.application.usecases.CancelEventUseCaseIT"`

Expected: PASS — os 3 testes de `CancelEventTicketsQueueIT` e os 3 de `CancelEventUseCaseIT` (que estava vermelho desde a Task 3).

- [ ] **Step 6: Rodar a suíte completa**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add infrastructure/src/main/java/br/com/fullcycle/infrastructure/configurations/UseCaseConfig.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java infrastructure/src/test/java/br/com/fullcycle/infrastructure/gateways/CancelEventTicketsQueueIT.java infrastructure/src/test/java/br/com/fullcycle/application/usecases/CancelEventUseCaseIT.java
git commit -m "feat: roteia o evento de dominio event.cancelled para a cascata de cancelamento de tickets"
```

---

## Task 7: Exposição REST com presenters

Dois presenters para o mesmo `Output`, alternados por header — o mesmo modelo do `CustomerController`, com duas diferenças ditadas pela spec: a representação pública é um JSON reduzido `{id, status}` (não uma string), e **os dois** presenters respondem 404 quando o evento não existe.

**Files:**
- Create: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/GetEventByIdResponseEntity.java`
- Create: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/PublicGetEventByIdJson.java`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/EventController.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/infrastructure/rest/EventControllerTest.java`

**Interfaces:**
- Consumes: `CancelEventUseCase` (Task 3), `GetEventByIdUseCase` (Task 5), beans da Task 6.
- Produces:
  - Bean `privateGetEvent` → `GetEventByIdResponseEntity implements Presenter<Optional<GetEventByIdUseCase.Output>, Object>`
  - Bean `publicGetEvent` → `PublicGetEventByIdJson implements Presenter<Optional<GetEventByIdUseCase.Output>, Object>`, com o record aninhado `PublicGetEventByIdJson.PublicEvent(String id, String status)`
  - `POST /events/{id}/cancel` e `GET /events/{id}`

O Spring distingue os quatro beans `Presenter<...>` (dois de customer, dois de event) pelos tipos genéricos declarados em cada classe, e escolhe entre os dois de event pelo nome do parâmetro do construtor — por isso os nomes de bean e de parâmetro têm que casar exatamente.

- [ ] **Step 1: Escrever os testes que falham**

Adicione estes quatro testes ao final de `infrastructure/src/test/java/br/com/fullcycle/infrastructure/rest/EventControllerTest.java` (antes do `}` final), mantendo os dois existentes intactos:

```java
    @Test
    @DisplayName("Deve cancelar um evento")
    public void testCancel() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        var eventId = mapper.readValue(createResult, CreateEventUseCase.Output.class).id();

        this.mvc.perform(
                        MockMvcRequestBuilders.post("/events/{id}/cancel", eventId)
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(eventId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CANCELLED"));

        var actualEvent = eventRepository.eventOfId(EventId.with(eventId)).get();
        Assertions.assertTrue(actualEvent.isCancelled());
    }

    @Test
    @DisplayName("Não deve cancelar um evento duas vezes")
    public void testCancelTwice() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        var eventId = mapper.readValue(createResult, CreateEventUseCase.Output.class).id();

        this.mvc.perform(MockMvcRequestBuilders.post("/events/{id}/cancel", eventId))
                .andExpect(MockMvcResultMatchers.status().isOk());

        this.mvc.perform(MockMvcRequestBuilders.post("/events/{id}/cancel", eventId))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().string("Event already cancelled"));
    }

    @Test
    @DisplayName("Deve obter um evento por id")
    public void testGet() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        var eventId = mapper.readValue(createResult, CreateEventUseCase.Output.class).id();

        this.mvc.perform(MockMvcRequestBuilders.get("/events/{id}", eventId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(eventId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Disney on Ice"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.date").value("2021-01-01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalSpots").value(100))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Deve obter um evento por id com X-Public")
    public void testGetPublic() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        var eventId = mapper.readValue(createResult, CreateEventUseCase.Output.class).id();

        this.mvc.perform(
                        MockMvcRequestBuilders.get("/events/{id}", eventId)
                                .header("X-Public", "true")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(eventId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalSpots").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar 404 ao obter um evento que não existe")
    public void testGetNotFound() throws Exception {

        var eventId = EventId.unique().value();

        this.mvc.perform(MockMvcRequestBuilders.get("/events/{id}", eventId))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        this.mvc.perform(
                        MockMvcRequestBuilders.get("/events/{id}", eventId)
                                .header("X-Public", "true")
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.rest.EventControllerTest"`

Expected: FAIL — `testCancel`, `testCancelTwice`, `testGet` e `testGetPublic` falham porque nenhuma rota casa com `POST /events/{id}/cancel` nem com `GET /events/{id}`: o Spring responde `404` e as asserções de `status().isOk()` quebram. (`testGetNotFound` passa por acidente, já que sem rota mapeada o 404 que ele espera é o que acontece; os outros quatro são os que provam o trabalho.)

- [ ] **Step 3: Criar o presenter completo**

Arquivo novo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/GetEventByIdResponseEntity.java`:

```java
package br.com.fullcycle.infrastructure.rest.presenters;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("privateGetEvent")
public class GetEventByIdResponseEntity implements Presenter<Optional<GetEventByIdUseCase.Output>, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(GetEventByIdResponseEntity.class);

    @Override
    public ResponseEntity<?> present(final Optional<GetEventByIdUseCase.Output> output) {
        return output.map(ResponseEntity::ok)
                .orElseGet(ResponseEntity.notFound()::build);
    }

    @Override
    public ResponseEntity<?> present(Throwable error) {
        LOG.error("An error was observer at GetEventByIdUseCase", error);
        return ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 4: Criar o presenter público**

Arquivo novo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/PublicGetEventByIdJson.java`:

```java
package br.com.fullcycle.infrastructure.rest.presenters;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("publicGetEvent")
public class PublicGetEventByIdJson implements Presenter<Optional<GetEventByIdUseCase.Output>, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(PublicGetEventByIdJson.class);

    @Override
    public ResponseEntity<?> present(final Optional<GetEventByIdUseCase.Output> output) {
        return output.map(o -> ResponseEntity.ok(new PublicEvent(o.id(), o.status())))
                .orElseGet(ResponseEntity.notFound()::build);
    }

    @Override
    public ResponseEntity<?> present(Throwable error) {
        LOG.error("An error was observer at GetEventByIdUseCase", error);
        return ResponseEntity.notFound().build();
    }

    public record PublicEvent(String id, String status) {
    }
}
```

- [ ] **Step 5: Expor as duas rotas no `EventController`**

Arquivo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/EventController.java` — arquivo completo:

```java
package br.com.fullcycle.infrastructure.rest;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.application.event.CreateEventUseCase;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.application.event.SubscribeCustomerToEventUseCase;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.infrastructure.dtos.NewEventDTO;
import br.com.fullcycle.infrastructure.dtos.SubscribeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

// Adapter
@RestController
@RequestMapping(value = "events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase;
    private final CancelEventUseCase cancelEventUseCase;
    private final GetEventByIdUseCase getEventByIdUseCase;
    private final Presenter<Optional<GetEventByIdUseCase.Output>, Object> privateGetEventPresenter;
    private final Presenter<Optional<GetEventByIdUseCase.Output>, Object> publicGetEventPresenter;

    public EventController(
            final CreateEventUseCase createEventUseCase,
            final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase,
            final CancelEventUseCase cancelEventUseCase,
            final GetEventByIdUseCase getEventByIdUseCase,
            final Presenter<Optional<GetEventByIdUseCase.Output>, Object> privateGetEvent,
            final Presenter<Optional<GetEventByIdUseCase.Output>, Object> publicGetEvent
    ) {
        this.createEventUseCase = Objects.requireNonNull(createEventUseCase);
        this.subscribeCustomerToEventUseCase = Objects.requireNonNull(subscribeCustomerToEventUseCase);
        this.cancelEventUseCase = Objects.requireNonNull(cancelEventUseCase);
        this.getEventByIdUseCase = Objects.requireNonNull(getEventByIdUseCase);
        this.privateGetEventPresenter = Objects.requireNonNull(privateGetEvent);
        this.publicGetEventPresenter = Objects.requireNonNull(publicGetEvent);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> create(@RequestBody NewEventDTO dto) {
        try {
            final var output =
                    createEventUseCase.execute(new CreateEventUseCase.Input(dto.date(), dto.name(), dto.partnerId(), dto.totalSpots()));

            return ResponseEntity.created(URI.create("/events/" + output.id())).body(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/{id}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable String id, @RequestBody SubscribeDTO dto) {
        try {
            final var output =
                    subscribeCustomerToEventUseCase.execute(new SubscribeCustomerToEventUseCase.Input(dto.customerId(), id));

            return ResponseEntity.ok(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        try {
            final var output = cancelEventUseCase.execute(new CancelEventUseCase.Input(id));

            return ResponseEntity.ok(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @GetMapping(value = "/{id}")
    public Object get(@PathVariable String id, @RequestHeader(name = "X-Public", required = false) String xPublic) {
        Presenter<Optional<GetEventByIdUseCase.Output>, Object> presenter = privateGetEventPresenter;

        if (xPublic != null) {
            presenter = publicGetEventPresenter;
        }

        return getEventByIdUseCase.execute(new GetEventByIdUseCase.Input(id), presenter);
    }
}
```

- [ ] **Step 6: Rodar os testes do controller e confirmar que passam**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.rest.EventControllerTest"`

Expected: PASS — os 2 testes antigos e os 5 novos.

Note que `testGetNotFound` com um `EventId.unique()` válido cai no `Optional.empty()` do caso de uso, e com um id malformado cairia no `present(Throwable)` (o `UseCase.execute(input, presenter)` captura o `ValidationException` de `EventId.with`) — os dois caminhos levam a 404, como a spec pede.

- [ ] **Step 7: Rodar a suíte completa**

Run: `./gradlew test`

Expected: PASS. Em particular `CustomerControllerTest` continua verde — prova de que acrescentar dois beans `Presenter` não ambiguou a injeção dos presenters de customer.

- [ ] **Step 8: Commit**

```bash
git add infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/GetEventByIdResponseEntity.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/presenters/PublicGetEventByIdJson.java infrastructure/src/main/java/br/com/fullcycle/infrastructure/rest/EventController.java infrastructure/src/test/java/br/com/fullcycle/infrastructure/rest/EventControllerTest.java
git commit -m "feat: expoe cancelamento e consulta de evento via REST com presenters"
```

---

## Task 8: Exposição GraphQL

O mesmo caso de uso dirigido por outro driver.

**Files:**
- Modify: `infrastructure/src/main/resources/graphql/schema.gqls`
- Modify: `infrastructure/src/main/java/br/com/fullcycle/infrastructure/graphql/EventResolver.java`
- Test: `infrastructure/src/test/java/br/com/fullcycle/infrastructure/graphql/EventResolverTest.java`

**Interfaces:**
- Consumes: `CancelEventUseCase` (Task 3), `GetEventByIdUseCase` (Task 5), beans da Task 6.
- Produces: mutation `cancelEvent(id: ID!): CancelEventResult!` e query `eventOfId(id: ID!): Event`.

- [ ] **Step 1: Escrever o teste que falha**

Arquivo novo `infrastructure/src/test/java/br/com/fullcycle/infrastructure/graphql/EventResolverTest.java`:

```java
package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureGraphQlTester
@SpringBootTest
class EventResolverTest {

    @Autowired
    private GraphQlTester graphql;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    private Partner disney;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();

        disney = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
    }

    @Test
    @DisplayName("Deve cancelar um evento via GraphQL")
    public void testCancelEvent() throws Exception {
        // given
        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));

        final var query = """
                mutation CancelEvent($id: ID!) {
                    cancelEvent(id: $id) {
                        id
                        status
                    }
                }
                """;

        // when
        this.graphql.document(query)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("cancelEvent.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("cancelEvent.status").entity(String.class).isEqualTo("CANCELLED");

        // then
        final var actualEvent = eventRepository.eventOfId(EventId.with(anEvent.eventId().value())).get();
        Assertions.assertTrue(actualEvent.isCancelled());
    }

    @Test
    @DisplayName("Deve obter um evento por id via GraphQL")
    public void testEventOfId() throws Exception {
        // given
        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));

        final var query = """
                query EventOfId($id: ID!) {
                    eventOfId(id: $id) {
                        id
                        name
                        date
                        totalSpots
                        status
                    }
                }
                """;

        // when / then
        this.graphql.document(query)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("eventOfId.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("eventOfId.name").entity(String.class).isEqualTo("Disney on Ice")
                .path("eventOfId.date").entity(String.class).isEqualTo("2021-01-01")
                .path("eventOfId.totalSpots").entity(Integer.class).isEqualTo(100)
                .path("eventOfId.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Deve criar um evento via GraphQL sem quebrar com o novo campo status")
    public void testCreateEventStillWorks() throws Exception {
        // given
        final var query = """
                mutation CreateEvent($input: EventInput) {
                    createEvent(input: $input) {
                        id
                        name
                        date
                        totalSpots
                    }
                }
                """;

        // when / then
        this.graphql.document(query)
                .variable("input", java.util.Map.of(
                        "date", "2021-01-01",
                        "name", "Disney on Ice",
                        "totalSpots", 100,
                        "partnerId", disney.partnerId().value()
                ))
                .execute()
                .path("createEvent.name").entity(String.class).isEqualTo("Disney on Ice")
                .path("createEvent.totalSpots").entity(Integer.class).isEqualTo(100);
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.graphql.EventResolverTest"`

Expected: FAIL — `testCancelEvent` e `testEventOfId` falham com erro de validação da query GraphQL (`Field 'cancelEvent' in type 'Mutation' is undefined`, `Field 'eventOfId' in type 'Query' is undefined`). `testCreateEventStillWorks` passa — é o teste de regressão.

- [ ] **Step 3: Evoluir o schema**

Arquivo `infrastructure/src/main/resources/graphql/schema.gqls` — arquivo completo. `status` entra em `Event` como **opcional**: o `type Event` é o retorno de `createEvent`, cujo Output não tem esse campo.

```graphql
type Query {
    customerOfId(id: ID!): Customer
    partnerOfId(id: ID!): Partner
    eventOfId(id: ID!): Event
}

type Mutation {
    createCustomer(input: CustomerInput): Customer!
    createEvent(input: EventInput): Event!
    createPartner(input: PartnerInput): Partner!
    subscribeCustomerToEvent(input: SubscribeInput): Subscribe!
    cancelEvent(id: ID!): CancelEventResult!
}

type Customer {
    id: ID!
    name: String
    email: String
    cpf: String
}

input CustomerInput {
    name: String
    email: String
    cpf: String
}

type Event {
    id: ID!
    date: String!
    totalSpots: Int!
    name: String!
    status: String
}

input EventInput {
    date: String
    totalSpots: Int
    name: String
    partnerId: ID
}

type CancelEventResult {
    id: ID!
    status: String!
}

type Partner {
    id: ID!
    name: String
    email: String
    cnpj: String
}

input PartnerInput {
    name: String
    email: String
    cnpj: String
}

type Subscribe {
    eventId: ID!
    ticketStatus: String!
    reservationDate: String!
}

input SubscribeInput {
    customerId: ID
    eventId: ID
}
```

- [ ] **Step 4: Adicionar as duas operações ao `EventResolver`**

Arquivo `infrastructure/src/main/java/br/com/fullcycle/infrastructure/graphql/EventResolver.java` — arquivo completo. `eventOfId` devolve `null` no `Optional` vazio, igual ao `customerOfId` do `CustomerResolver`, e o GraphQL responde `"eventOfId": null`:

```java
package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.application.event.CreateEventUseCase;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.application.event.SubscribeCustomerToEventUseCase;
import br.com.fullcycle.infrastructure.dtos.NewEventDTO;
import br.com.fullcycle.infrastructure.dtos.SubscribeDTO;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

// Adapter
@Controller
public class EventResolver {

    private final CreateEventUseCase createEventUseCase;
    private final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase;
    private final CancelEventUseCase cancelEventUseCase;
    private final GetEventByIdUseCase getEventByIdUseCase;

    public EventResolver(
            final CreateEventUseCase createEventUseCase,
            final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase,
            final CancelEventUseCase cancelEventUseCase,
            final GetEventByIdUseCase getEventByIdUseCase
    ) {
        this.createEventUseCase = Objects.requireNonNull(createEventUseCase);
        this.subscribeCustomerToEventUseCase = Objects.requireNonNull(subscribeCustomerToEventUseCase);
        this.cancelEventUseCase = Objects.requireNonNull(cancelEventUseCase);
        this.getEventByIdUseCase = Objects.requireNonNull(getEventByIdUseCase);
    }

    @MutationMapping
    public CreateEventUseCase.Output createEvent(@Argument NewEventDTO input) {
        return createEventUseCase.execute(new CreateEventUseCase.Input(input.date(), input.name(), input.partnerId(), input.totalSpots()));
    }

    @Transactional
    @MutationMapping
    public SubscribeCustomerToEventUseCase.Output subscribeCustomerToEvent(@Argument SubscribeDTO input) {
        return subscribeCustomerToEventUseCase.execute(new SubscribeCustomerToEventUseCase.Input(input.customerId(), input.eventId()));
    }

    @MutationMapping
    public CancelEventUseCase.Output cancelEvent(@Argument String id) {
        return cancelEventUseCase.execute(new CancelEventUseCase.Input(id));
    }

    @QueryMapping
    public GetEventByIdUseCase.Output eventOfId(@Argument String id) {
        return getEventByIdUseCase.execute(new GetEventByIdUseCase.Input(id)).orElse(null);
    }
}
```

- [ ] **Step 5: Rodar o teste GraphQL e confirmar que passa**

Run: `./gradlew :infrastructure:test --tests "br.com.fullcycle.infrastructure.graphql.EventResolverTest"`

Expected: PASS — os 3 testes.

- [ ] **Step 6: Rodar a suíte completa**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add infrastructure/src/main/resources/graphql/schema.gqls infrastructure/src/main/java/br/com/fullcycle/infrastructure/graphql/EventResolver.java infrastructure/src/test/java/br/com/fullcycle/infrastructure/graphql/EventResolverTest.java
git commit -m "feat: expoe cancelamento e consulta de evento via GraphQL"
```

---

## Task 9: README e verificação final

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: tudo das tasks anteriores.
- Produces: nada de código.

- [ ] **Step 1: Escrever o README**

Arquivo novo `README.md` na raiz do repositório:

```markdown
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
```

- [ ] **Step 2: Rodar a suíte completa e confirmar que tudo passa**

Run: `./gradlew clean test`

Expected: PASS. Confira na saída que nenhum módulo tem teste falhando ou ignorado.

- [ ] **Step 3: Verificar as restrições de arquitetura**

Rode estas buscas e confirme que todas voltam vazias:

```bash
git grep -n "org.springframework" -- domain/src/main
git grep -n "jakarta.persistence" -- domain/src/main
git grep -n "org.springframework" -- application/src/main
git grep -n "infrastructure" -- application/src/main
```

Expected: nenhuma linha. O `domain` não tem framework; o `application` não conhece Spring nem infraestrutura.

Confirme também que `CancelEventUseCase` não referencia ingresso nenhum:

```bash
git grep -ni "ticket" -- application/src/main/java/br/com/fullcycle/application/event/CancelEventUseCase.java
```

Expected: nenhuma linha. A cascata só acontece por reação ao evento de domínio.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: adiciona README com instrucoes de execucao e o caminho da cascata de cancelamento"
```

---

## Checklist de critérios de aceite

Confira cada item contra o código antes de considerar a entrega pronta.

### Domínio
- [ ] `Event` tem estado de cancelamento (`EventStatus`) e `cancel()` que registra `EventCancelled` — Task 1
- [ ] Cancelar evento já cancelado lança `ValidationException("Event already cancelled")` — Task 1
- [ ] Reservar lugar em evento cancelado lança `ValidationException("Event is cancelled")` — Task 1
- [ ] `Ticket.cancel()` existe e `TicketStatus` inclui `CANCELLED` — Task 2
- [ ] `EventCancelled` implementa `DomainEvent` com `type = "event.cancelled"` — Task 1
- [ ] Nenhuma anotação de framework em `domain` — Task 9 Step 3

### Comando e cascata
- [ ] `CancelEventUseCase` com `Input`/`Output` próprios, exposto por REST e GraphQL — Tasks 3, 7, 8
- [ ] Cancelamento dos ingressos é assíncrono, reagindo ao `EventCancelled` pela fila, sem o agregado de Evento tocar o de Ingresso e sem cancelamento síncrono no comando — Tasks 4, 6, verificado na Task 9 Step 3
- [ ] `ConsumerQueueGateway` roteia `event.cancelled` para `CancelEventTicketsUseCase` — Task 6
- [ ] `TicketRepository.ticketsByEventId` implementado no adapter in-memory e no de banco — Task 2
- [ ] Após a cascata todos os ingressos do evento estão `CANCELLED`, e reprocessar é idempotente — Tasks 4, 6

### Consulta e presenter
- [ ] `GetEventByIdUseCase` expõe o estado do evento — Task 5
- [ ] Dois presenters para a mesma saída, alternados por header via `execute(input, presenter)`; público devolve `{id, status}`; ambos respondem 404 quando o evento não existe — Task 7

### Drivers e composição
- [ ] Cancelamento e consulta expostos por REST e GraphQL, com `status` no `type Event` — Tasks 7, 8
- [ ] Novos casos de uso registrados em `UseCaseConfig` — Task 6

### Testes
- [ ] Testes de domínio das novas regras, incluindo registro do evento em `allDomainEvents()` — Tasks 1, 2
- [ ] Testes de caso de uso com repositórios in-memory, caminhos felizes e de erro — Tasks 3, 4, 5
- [ ] Testes de integração com persistência real, incluindo busca de ingressos por evento — Tasks 2, 3
- [ ] Teste ponta-a-ponta que dispara o `EventCancelled` pelo caminho real e comprova que os ingressos terminam `CANCELLED` — Task 6
- [ ] A suíte existente continua passando — verificado ao fim de cada task

### Entregável
- [ ] README com instruções de execução, de testes, e o parágrafo sobre onde a cascata acontece — Task 9
- [ ] Entrega consolidada na branch `main` do fork público
