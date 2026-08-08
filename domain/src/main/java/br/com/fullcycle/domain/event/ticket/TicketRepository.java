package br.com.fullcycle.domain.event.ticket;

import br.com.fullcycle.domain.event.EventId;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> ticketOfId(TicketId anId);

    List<Ticket> ticketsByEventId(EventId eventId);

    Ticket create(Ticket ticket);

    Ticket update(Ticket ticket);

    void deleteAll();
}
