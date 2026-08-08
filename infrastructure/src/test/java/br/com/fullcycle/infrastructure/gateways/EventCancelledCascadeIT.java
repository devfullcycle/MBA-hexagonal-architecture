package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.await;

class EventCancelledCascadeIT extends IntegrationTest {

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private CancelEventUseCase cancelEventUseCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento ao receber EventCancelled pela fila")
    public void testEventCancelledCascade() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var anEventId = anEvent.eventId();

        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));

        cancelEventUseCase.execute(new CancelEventUseCase.Input(anEventId.value()));

        final var eventCancelledContent = StreamSupport.stream(outboxJpaRepository.findAll().spliterator(), false)
                .filter(it -> !it.published())
                .map(OutboxEntity::content)
                .filter(content -> content.contains("event.cancelled"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("EventCancelled not found in outbox"));

        // when
        consumerQueueGateway.publish(eventCancelledContent);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            final var actualTickets = ticketRepository.ticketsByEventId(anEventId);
            Assertions.assertEquals(2, actualTickets.size());
            Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED));
        });
    }
}
