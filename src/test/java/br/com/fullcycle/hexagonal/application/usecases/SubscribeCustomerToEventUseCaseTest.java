package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.models.Customer;
import br.com.fullcycle.hexagonal.models.Event;
import br.com.fullcycle.hexagonal.models.Ticket;
import br.com.fullcycle.hexagonal.models.TicketStatus;
import br.com.fullcycle.hexagonal.services.CustomerService;
import br.com.fullcycle.hexagonal.services.EventService;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscribeCustomerToEventUseCaseTest {

    @Test
    @DisplayName("Deve comprar um ticket de um evento")
    public void testReserveTicket() throws Exception {
        // given
        final var expectedTicketsSize = 1;

        final var customerID = TSID.fast().toLong();
        final var eventID = TSID.fast().toLong();

        final var aEvent = new Event();
        aEvent.setId(eventID);
        aEvent.setName("Disney");
        aEvent.setTotalSpots(10);

        final var subscribeInput =
                new SubscribeCustomerToEventUseCase.Input(customerID, aEvent.getId());

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerID)).thenReturn(Optional.of(new Customer()));
        when(eventService.findById(eventID)).thenReturn(Optional.of(aEvent));
        when(eventService.findTicketByEventIdAndCustomerId(eventID, customerID)).thenReturn(Optional.empty());
        when(eventService.save(any())).thenAnswer(a -> {
            final var e = a.getArgument(0, Event.class);
            Assertions.assertEquals(expectedTicketsSize, e.getTickets().size());
            return e;
        });

        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var output = useCase.execute(subscribeInput);

        // then
        Assertions.assertEquals(eventID, output.eventId());
        Assertions.assertNotNull(output.reservationDate());
        Assertions.assertEquals(TicketStatus.PENDING.name(), output.ticketStatus());
    }

    @Test
    @DisplayName("Não deve comprar um ticket com um cliente não existente")
    public void testReserveTicketWithoutCustomer() throws Exception {
        // given
        final var expectedError = "Customer not found";

        final var customerID = TSID.fast().toLong();
        final var eventID = TSID.fast().toLong();

        final var subscribeInput =
                new SubscribeCustomerToEventUseCase.Input(customerID, eventID);

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerID)).thenReturn(Optional.empty());

        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(subscribeInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve comprar um ticket de um evento que não existe")
    public void testReserveTicketWithoutEvent() throws Exception {
        // given
        final var expectedError = "Event not found";

        final var customerID = TSID.fast().toLong();
        final var eventID = TSID.fast().toLong();

        final var subscribeInput =
                new SubscribeCustomerToEventUseCase.Input(customerID, eventID);

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerID)).thenReturn(Optional.of(new Customer()));
        when(eventService.findById(eventID)).thenReturn(Optional.empty());

        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(subscribeInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Um mesmo cliente não pode comprar mais de um ticket por evento")
    public void testReserveTicketMoreThanOnce() throws Exception {
        // given
        final var expectedError = "Email already registered";

        final var customerID = TSID.fast().toLong();
        final var eventID = TSID.fast().toLong();

        final var aEvent = new Event();
        aEvent.setId(eventID);
        aEvent.setName("Disney");
        aEvent.setTotalSpots(10);

        final var subscribeInput =
                new SubscribeCustomerToEventUseCase.Input(customerID, aEvent.getId());

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerID)).thenReturn(Optional.of(new Customer()));
        when(eventService.findById(eventID)).thenReturn(Optional.of(aEvent));
        when(eventService.findTicketByEventIdAndCustomerId(eventID, customerID)).thenReturn(Optional.of(new Ticket()));

        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(subscribeInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Um mesmo cliente não pode comprar de um evento que não há mais cadeiras")
    public void testReserveTicketWithoutSlots() throws Exception {
        // given
        final var expectedError = "Event sold out";

        final var customerID = TSID.fast().toLong();
        final var eventID = TSID.fast().toLong();

        final var aEvent = new Event();
        aEvent.setId(eventID);
        aEvent.setName("Disney");
        aEvent.setTotalSpots(0);

        final var subscribeInput =
                new SubscribeCustomerToEventUseCase.Input(customerID, aEvent.getId());

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerID)).thenReturn(Optional.of(new Customer()));
        when(eventService.findById(eventID)).thenReturn(Optional.of(aEvent));
        when(eventService.findTicketByEventIdAndCustomerId(eventID, customerID)).thenReturn(Optional.empty());

        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(subscribeInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}