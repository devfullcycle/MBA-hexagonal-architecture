package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var anEventId = EventId.unique();

        final var aTicket1 = Ticket.newTicket(CustomerId.unique(), anEventId);
        final var aTicket2 = Ticket.newTicket(CustomerId.unique(), anEventId);

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEventId.value());

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(aTicket1);
        ticketRepository.create(aTicket2);

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(anEventId.value(), output.eventId());
        Assertions.assertEquals(2, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED));
    }

    @Test
    @DisplayName("Deve ser idempotente reprocessar o cancelamento dos tickets de um evento")
    public void testCancelEventTicketsIsIdempotent() throws Exception {
        // given
        final var anEventId = EventId.unique();

        final var aTicket = Ticket.newTicket(CustomerId.unique(), anEventId);

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEventId.value());

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(aTicket);

        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // when
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(1, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED));
    }

    @Test
    @DisplayName("Não deve quebrar ao cancelar os tickets de um evento sem tickets")
    public void testCancelEventTicketsWithoutTickets() throws Exception {
        // given
        final var anEventId = EventId.unique();

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEventId.value());

        final var ticketRepository = new InMemoryTicketRepository();

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(0, output.cancelledTickets());
    }
}
