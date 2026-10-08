package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var janeDoe = Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com");

        final var expectedCancelledTickets = 2;

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(janeDoe.customerId(), anEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.eventId());
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(expectedCancelledTickets, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> TicketStatus.CANCELLED == it.status()));
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar o cancelamento")
    public void testCancelEventTicketsTwice() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var expectedCancelledTickets = 1;

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // when
        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(cancelInput));

        // then
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        final var actualTickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> TicketStatus.CANCELLED == it.status()));
    }

    @Test
    @DisplayName("Não deve cancelar tickets de outros eventos")
    public void testCancelEventTicketsDoesNotTouchOtherEvents() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var anotherEvent = Event.newEvent("Frozen on Ice", "2021-02-01", 10, aPartner);
        final var johnDoe = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(johnDoe.customerId(), anotherEvent.eventId()));

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // then
        final var untouchedTickets = ticketRepository.ticketsByEventId(anotherEvent.eventId());
        Assertions.assertEquals(1, untouchedTickets.size());
        Assertions.assertEquals(TicketStatus.PENDING, untouchedTickets.get(0).status());
    }

    @Test
    @DisplayName("Deve concluir sem erro quando o evento não tem tickets")
    public void testCancelEventTicketsWithoutTickets() throws Exception {
        // given
        final var expectedCancelledTickets = 0;

        final var ticketRepository = new InMemoryTicketRepository();

        final var cancelInput = new CancelEventTicketsUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());
    }
}
