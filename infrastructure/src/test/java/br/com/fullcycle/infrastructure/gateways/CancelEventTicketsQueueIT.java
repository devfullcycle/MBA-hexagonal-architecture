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
                outbox.stream().anyMatch(it -> it.content().contains("event.cancelled")
                        && it.content().contains(anEvent.eventId().value())),
                "a outbox deveria conter o evento de dominio event.cancelled do evento cancelado"
        );
    }
}
