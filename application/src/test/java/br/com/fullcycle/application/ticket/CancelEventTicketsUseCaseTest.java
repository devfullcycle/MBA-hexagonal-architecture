package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var aTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());
        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(aTicket);

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(new CancelEventTicketsUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertEquals(1, output.cancelledTicketsCount());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(aTicket.ticketId()).get().status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar cancelamento dos tickets")
    public void testCancelEventTicketsTwice() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var aTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());
        final var ticketRepository = new InMemoryTicketRepository();
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        ticketRepository.create(aTicket);

        // when
        useCase.execute(new CancelEventTicketsUseCase.Input(anEvent.eventId().value()));
        final var output = useCase.execute(new CancelEventTicketsUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertEquals(0, output.cancelledTicketsCount());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(aTicket.ticketId()).get().status());
    }
}
