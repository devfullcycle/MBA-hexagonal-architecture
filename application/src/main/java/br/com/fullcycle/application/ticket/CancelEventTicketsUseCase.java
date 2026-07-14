package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.exceptions.ValidationException;

import java.util.Objects;

public class CancelEventTicketsUseCase
        extends UseCase<CancelEventTicketsUseCase.Input, CancelEventTicketsUseCase.Output> {

    private final TicketRepository ticketRepository;

    public CancelEventTicketsUseCase(final TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
    }

    @Override
    public Output execute(final Input input) {
        final var eventId = EventId.with(input.eventId);

        final var tickets = this.ticketRepository.ticketsOfEventId(eventId);

        if (tickets.isEmpty()) {
            return new Output(0);
        }

        int cancelledCount = 0;
        for (final var aTicket : tickets) {
            if (aTicket.status() != br.com.fullcycle.domain.event.ticket.TicketStatus.CANCELLED) {
                aTicket.cancel();
                this.ticketRepository.update(aTicket);
                cancelledCount++;
            }
        }

        return new Output(cancelledCount);
    }

    public record Input(String eventId) {
    }

    public record Output(int cancelledTicketsCount) {
    }
}
