package br.com.fullcycle.hexagonal.application.repositories;

import br.com.fullcycle.hexagonal.application.domain.Ticket;
import br.com.fullcycle.hexagonal.application.domain.TicketId;

import java.util.Optional;

public interface TicketRepository {

    Optional<Ticket> ticketOfId(TicketId anId);

    Ticket create(Ticket ticket);

    Ticket update(Ticket ticket);

}
