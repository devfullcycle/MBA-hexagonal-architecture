package br.com.fullcycle.hexagonal.application.domain.event.ticket;

import br.com.fullcycle.hexagonal.application.domain.customer.Customer;
import br.com.fullcycle.hexagonal.application.domain.event.Event;
import br.com.fullcycle.hexagonal.application.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TicketTest {

    @Test
    @DisplayName("Deve criar um ticket")
    public void testReserveTicket() throws Exception {
        // given
        final var aPartner =
                Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        final var aCustomer =
                Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var anEvent =
                Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedTickets = 1;
        final var expectedTicketOrder = 1;
        final var expectedTicketStatus = TicketStatus.PENDING;
        final var expectedEventId = anEvent.eventId();
        final var expectedCustomerId = aCustomer.customerId();

        // when
        final var actualTicket = Ticket.newTicket(aCustomer.customerId(), anEvent.eventId());

        // then
        Assertions.assertNotNull(actualTicket.ticketId());
        Assertions.assertNotNull(actualTicket.reservedAt());
        Assertions.assertNull(actualTicket.paidAt());
        Assertions.assertEquals(expectedEventId, actualTicket.eventId());
        Assertions.assertEquals(expectedCustomerId, actualTicket.customerId());
        Assertions.assertEquals(expectedTicketStatus, actualTicket.status());
    }
}