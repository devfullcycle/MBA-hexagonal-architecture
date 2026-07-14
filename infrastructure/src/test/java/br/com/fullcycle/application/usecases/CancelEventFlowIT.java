package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.gateways.QueueGateway;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventFlowIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase cancelEventUseCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private QueueGateway queueGateway;

    @Autowired
    private ObjectMapper mapper;

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
    @DisplayName("Deve persistir o status cancelado do evento")
    public void testCancelEventPersistence() {
        // given
        final var partner = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, partner));

        // when
        final var output = cancelEventUseCase.execute(new CancelEventUseCase.Input(event.eventId().value()));

        // then
        Assertions.assertEquals("CANCELLED", output.status());
        final var actualEvent = eventRepository.eventOfId(event.eventId()).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());
    }

    @Test
    @DisplayName("Deve buscar tickets por evento na persistÃªncia real")
    public void testTicketsOfEventId() {
        // given
        final var partner = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, partner));
        final var customer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-00", "john@gmail.com"));
        final var ticket = ticketRepository.create(Ticket.newTicket(customer.customerId(), event.eventId()));

        // when
        final var tickets = ticketRepository.ticketsOfEventId(event.eventId());

        // then
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(ticket.ticketId(), tickets.get(0).ticketId());
    }

    @Test
    @DisplayName("Deve cancelar tickets ao consumir EventCancelled pela fila")
    public void testCancelEventTicketsByQueue() throws Exception {
        // given
        final var partner = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, partner));
        final var customer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-00", "john@gmail.com"));
        final var ticket = ticketRepository.create(Ticket.newTicket(customer.customerId(), event.eventId()));
        final var eventCancelled = new EventCancelled(event.eventId());

        // when
        queueGateway.publish(mapper.writeValueAsString(eventCancelled));

        // then
        waitUntilTicketIsCancelled(ticket);
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket.ticketId()).get().status());

        queueGateway.publish(mapper.writeValueAsString(eventCancelled));
        waitUntilTicketIsCancelled(ticket);
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket.ticketId()).get().status());
    }

    private void waitUntilTicketIsCancelled(final Ticket ticket) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            final var actualTicket = ticketRepository.ticketOfId(ticket.ticketId()).get();
            if (TicketStatus.CANCELLED.equals(actualTicket.status())) {
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Ticket was not cancelled by the queue");
    }
}
