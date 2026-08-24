package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
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
    @DisplayName("Deve buscar os tickets de um evento")
    public void testTicketsByEventId() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));
        final var anotherEvent = eventRepository.create(Event.newEvent("Rock in Rio", "2021-02-01", 10, aPartner));

        final var aCustomer = customerRepository.create(Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com"));
        final var aCustomer2 = customerRepository.create(Customer.newCustomer("Pedro Doe", "123.111.789-01", "pedro.doe@gmail.com"));

        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(aCustomer2.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anotherEvent.eventId()));

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());

        // then
        Assertions.assertEquals(2, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.eventId().equals(anEvent.eventId())));
    }

    @Test
    @DisplayName("Deve retornar lista vazia para um evento sem tickets")
    public void testTicketsByEventIdWithoutTickets() throws Exception {
        // when
        final var actualTickets = ticketRepository.ticketsByEventId(EventId.unique());

        // then
        Assertions.assertTrue(actualTickets.isEmpty());
    }
}
