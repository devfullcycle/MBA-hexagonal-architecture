package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar os tickets de um evento")
    public void testTicketsByEventId() throws Exception {
        // given
        final var anEventId = EventId.unique();
        final var anotherEventId = EventId.unique();

        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anEventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), anotherEventId));

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);

        // then
        Assertions.assertEquals(2, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.eventId().equals(anEventId)));
    }

    @Test
    @DisplayName("Deve retornar vazio quando o evento não possui tickets")
    public void testTicketsByEventIdWithoutTickets() throws Exception {
        // when
        final var actualTickets = ticketRepository.ticketsByEventId(EventId.unique());

        // then
        Assertions.assertTrue(actualTickets.isEmpty());
    }
}
