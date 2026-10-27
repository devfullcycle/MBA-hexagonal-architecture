package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.models.Customer;
import br.com.fullcycle.hexagonal.models.Event;
import br.com.fullcycle.hexagonal.models.TicketStatus;
import br.com.fullcycle.hexagonal.services.CustomerService;
import br.com.fullcycle.hexagonal.services.EventService;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscribeCustomerToEventUseCaseTest {

    @Test
    @DisplayName("Deve comprar um ticket de um evento")
    public void testReserveTicket() {

        // given
        final var expectedTicketSize = 1;
        final var customerId = TSID.fast().toLong();
        final var eventId = TSID.fast().toLong();

        final var aCustomer = new Customer();
        aCustomer.setId(customerId);
        aCustomer.setCpf("12345678901");
        aCustomer.setEmail("john.doe@gmail.com");
        aCustomer.setName("John Doe");

        final var aEvent = new Event();
        aEvent.setId(eventId);
        aEvent.setName("Disney");
        aEvent.setTotalSpots(10);

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerId)).thenReturn(Optional.of(aCustomer));
        when(eventService.findById(eventId)).thenReturn(Optional.of(aEvent));
        when(eventService.findTicketByEventIdAndCustomerId(eventId, customerId))
                .thenReturn(Optional.empty());
        when(eventService.save(any())).thenAnswer(a -> {
            final var e = a.getArgument(0, Event.class);
            assertEquals(expectedTicketSize, e.getTickets().size());
            return e;
        });

        final var input = new SubscribeCustomerToEventUseCase.Input(customerId, eventId);
        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var output = useCase.execute(input);

        // then
        assertEquals(eventId, output.eventId());
        assertNotNull(output.reservationDate());
        assertEquals(TicketStatus.PENDING.name(), output.ticketStatus());
    }

    @Test
    @DisplayName("não deve comprar um ticket com um client não existente")
    public void notShouldBuyTicketOfClientDoesntExists() {

        // given
        final var expectedError = "Customer not found";
        final var customerId = TSID.fast().toLong();
        final var eventId = TSID.fast().toLong();

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerId)).thenReturn(Optional.empty());

        final var input = new SubscribeCustomerToEventUseCase.Input(customerId, eventId);
        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var output = assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        verify(eventService, never()).findById(any());
        verify(eventService, never()).findTicketByEventIdAndCustomerId(any(), any());
        verify(eventService, never()).save(any());
        assertEquals(expectedError, output.getMessage());
    }

    @Test
    @DisplayName("não deve comprar um ticket de um evento que não existe")
    public void notShouldBuyTicketOfEventDoesntExists() {

        // given
        final var expectedError = "Event not found";
        final var customerId = TSID.fast().toLong();
        final var eventId = TSID.fast().toLong();

        final var aCustomer = new Customer();
        aCustomer.setId(customerId);
        aCustomer.setCpf("12345678901");
        aCustomer.setEmail("john.doe@gmail.com");
        aCustomer.setName("John Doe");

        // when
        final var customerService = mock(CustomerService.class);
        final var eventService = mock(EventService.class);

        when(customerService.findById(customerId)).thenReturn(Optional.of(aCustomer));
        when(eventService.findById(eventId)).thenReturn(Optional.empty());

        final var input = new SubscribeCustomerToEventUseCase.Input(customerId, eventId);
        final var useCase = new SubscribeCustomerToEventUseCase(customerService, eventService);
        final var output = assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        verify(eventService, never()).findTicketByEventIdAndCustomerId(any(), any());
        verify(eventService, never()).save(any());
        assertEquals(expectedError, output.getMessage());
    }
}