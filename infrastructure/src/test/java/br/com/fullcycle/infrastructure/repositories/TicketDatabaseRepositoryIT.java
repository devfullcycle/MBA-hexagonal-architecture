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
