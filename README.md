# MBA Hexagonal Architecture

Projeto exemplo de **Arquitetura Hexagonal** (também conhecida como Ports and Adapters) aplicado a um sistema de gestão de eventos e ingressos.

## Visão Geral

Este projeto foi desenvolvido para demonstrar os princípios da arquitetura hexagonal em Java, separando claramente as camadas de domínio, aplicação e infraestrutura. Segue os princípios de Domain-Driven Design (DDD) com agregados, entidades, use cases e inversão de dependências.

### Módulos

- **domain**: Contém as entidades de domínio, agregados, eventos de domínio, regras de negócio e exceções. É independente de frameworks.
- **application**: Implementa os casos de uso (Use Cases) que orquestram o fluxo de negócio. Depende apenas do módulo domain.
- **infrastructure**: Implementa os adapters (GraphQL, JPA, REST, etc.), configurações do Spring Boot, repositórios e integrações com o mundo externo.

## Funcionalidades Principais

- Cadastro de **Clientes** (Customer)
- Cadastro de **Parceiros** (Partner)
- Criação de **Eventos** (Event)
- Subscrição de Clientes em Eventos (SubscribeCustomerToEvent) com emissão de **Ingressos** (Tickets)
- Cancelamento de eventos e ingressos
- Consulta de evento por id com representação completa ou pública via header `X-Public`
- Consultas via GraphQL

## Tecnologias Utilizadas

- Java 17+
- Spring Boot 3.1
- Spring Data JPA + MySQL
- Spring GraphQL (com GraphiQL habilitado)
- Gradle (multi-módulo)
- Docker Compose (para banco de dados)

## Requisitos

- Java 17 ou superior
- Gradle Wrapper (incluído)
- Docker (para executar o MySQL via docker-compose)

## Como Executar

1. Inicie o banco de dados MySQL:

   ```bash
   docker-compose up -d
   ```

2. Execute a aplicação:

   ```bash
   ./gradlew :infrastructure:bootRun
   ```

3. Acesse a interface GraphiQL em:

   ```
   http://localhost:8080/graphiql
   ```

## Comandos Úteis

- Compilar todo o projeto:

  ```bash
  ./gradlew build
  ```

- Executar testes:

  ```bash
  ./gradlew test
  ```

- Limpar builds:

  ```bash
  ./gradlew clean
  ```

## Cancelamento de Eventos

O cancelamento começa no `CancelEventUseCase`, que carrega o agregado `Event` e chama `event.cancel()`. O agregado muda para `CANCELLED` e registra o evento de domínio `EventCancelled` com o tipo `event.cancelled`. Ao salvar o evento, o `EventDatabaseRepository` grava esse evento na outbox; o `OutboxRelay` publica o JSON na fila via `QueueGateway`; e o `ConsumerQueueGateway` roteia `event.cancelled` para o `CancelEventTicketsUseCase`, que busca os ingressos pelo `eventId` na porta `TicketRepository` e cancela todos de forma idempotente.

## Estrutura de Arquitetura

```
mba-hexagonal-arch/
├── domain/           # Núcleo do domínio (sem dependências externas)
├── application/      # Casos de uso (orquestração)
├── infrastructure/   # Adapters, controllers GraphQL, JPA, etc.
├── docker-compose.yml
└── settings.gradle.kts
```

## Próximos Passos / Melhorias

- Implementação completa de testes unitários e de integração
- Adição de comandos via REST API
- Integração com mensageria (ex: RabbitMQ ou Kafka) para eventos de domínio
- Containerização da aplicação com Docker

---

Desenvolvido como parte do MBA em Arquitetura Hexagonal.
