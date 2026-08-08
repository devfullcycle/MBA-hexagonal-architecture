package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureHttpGraphQlTester
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventResolverTest {

    @Autowired
    private HttpGraphQlTester graphQlTester;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar um evento via GraphQL")
    void testCancelEvent() {
        final var aPartner =
                partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        graphQlTester.document("""
                        mutation CancelEvent($id: ID!) {
                            cancelEvent(id: $id) {
                                id
                                status
                            }
                        }
                        """)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("cancelEvent.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("cancelEvent.status").entity(String.class).isEqualTo("CANCELLED");

        final var actualEvent = eventRepository.eventOfId(EventId.with(anEvent.eventId().value())).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());
    }

    @Test
    @DisplayName("Deve consultar um evento por id via GraphQL")
    void testEventOfId() {
        final var aPartner =
                partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        graphQlTester.document("""
                        query GetEvent($id: ID!) {
                            eventOfId(id: $id) {
                                id
                                name
                                date
                                totalSpots
                                status
                            }
                        }
                        """)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("eventOfId.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("eventOfId.name").entity(String.class).isEqualTo("Disney on Ice")
                .path("eventOfId.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Deve retornar nulo ao consultar um evento que não existe via GraphQL")
    void testEventOfIdNotFound() {
        graphQlTester.document("""
                        query GetEvent($id: ID!) {
                            eventOfId(id: $id) {
                                id
                            }
                        }
                        """)
                .variable("id", EventId.unique().value())
                .execute()
                .path("eventOfId").valueIsNull();
    }
}
