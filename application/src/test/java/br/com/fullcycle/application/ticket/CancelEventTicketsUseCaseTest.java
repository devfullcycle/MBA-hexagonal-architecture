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
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");
        final var aCustomer2 = Customer.newCustomer("Pedro Doe", "123.111.789-01", "pedro.doe@gmail.com");

        final var aTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());
        final var aTicket2 = Ticket.newTicket(aCustomer2.customerId(), anEvent.eventId());

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(aTicket);
        ticketRepository.create(aTicket2);

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(2, output.cancelledTicketIds().size());

        final var tickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertTrue(tickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED));
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar o cancelamento dos tickets de um evento")
    public void testCancelEventTicketsIsIdempotent() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");

        final var aTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(aTicket);

        final var cancelInput = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);

        useCase.execute(cancelInput);

        // when
        useCase.execute(cancelInput);

        // then
        final var tickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(TicketStatus.CANCELLED, tickets.get(0).status());
    }

    @Test
    @DisplayName("Não deve falhar ao cancelar os tickets de um evento sem tickets")
    public void testCancelEventTicketsWithoutTickets() throws Exception {
        // given
        final var ticketRepository = new InMemoryTicketRepository();
        final var cancelInput = new CancelEventTicketsUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertTrue(output.cancelledTicketIds().isEmpty());
    }
}
