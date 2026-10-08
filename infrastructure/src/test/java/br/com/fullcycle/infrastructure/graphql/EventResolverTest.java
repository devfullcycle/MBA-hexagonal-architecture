package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
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

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    private Partner disney;

    @BeforeEach
    void setUp() {
        outboxJpaRepository.deleteAll();
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
    @DisplayName("Deve retornar nulo ao obter um evento que não existe via GraphQL")
    public void testEventOfIdNotFound() throws Exception {
        // given
        final var query = """
                query EventOfId($id: ID!) {
                    eventOfId(id: $id) {
                        id
                        status
                    }
                }
                """;

        // when / then
        this.graphql.document(query)
                .variable("id", EventId.unique().value())
                .execute()
                .path("eventOfId").valueIsNull();
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
