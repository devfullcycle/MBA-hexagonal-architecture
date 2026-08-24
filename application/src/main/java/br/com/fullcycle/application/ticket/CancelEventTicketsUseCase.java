package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;

import java.util.List;
import java.util.Objects;

public class CancelEventTicketsUseCase
        extends UseCase<CancelEventTicketsUseCase.Input, CancelEventTicketsUseCase.Output> {

    private final TicketRepository ticketRepository;

    public CancelEventTicketsUseCase(final TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
    }

    @Override
    public Output execute(final Input input) {
        final var anEventId = EventId.with(input.eventId());

        final var cancelledTicketIds = ticketRepository.ticketsByEventId(anEventId).stream()
                .map(aTicket -> {
                    aTicket.cancel();
                    ticketRepository.update(aTicket);
                    return aTicket.ticketId().value();
                })
                .toList();

        return new Output(input.eventId(), cancelledTicketIds);
    }

    public record Input(String eventId) {
    }

    public record Output(String eventId, List<String> cancelledTicketIds) {
    }
}
