package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventUseCaseIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase useCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve persistir o evento como cancelado")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var expectedStatus = EventStatus.CANCELLED;

        // when
        final var output = useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.id());
        Assertions.assertEquals(expectedStatus.name(), output.status());

        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(expectedStatus, actualEvent.status());
        Assertions.assertTrue(actualEvent.isCancelled());
    }

    @Test
    @DisplayName("Não deve aceitar inscrição em um evento cancelado")
    public void testSubscribeToCancelledEvent() throws Exception {
        // given
        final var aPartner =
                partnerRepository.create(Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com"));

        final var johnDoe =
                customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));

        final var anEvent =
                eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var expectedError = "Event is cancelled";

        useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        final var actualEvent = eventRepository.eventOfId(EventId.with(anEvent.eventId().value())).get();

        // when
        final var actualException = Assertions.assertThrows(
                ValidationException.class,
                () -> actualEvent.reserveTicket(johnDoe.customerId())
        );

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEventWithoutEvent() throws Exception {
        // given
        final var expectedError = "Event not found";

        // when
        final var actualException = Assertions.assertThrows(
                ValidationException.class,
                () -> useCase.execute(new CancelEventUseCase.Input(EventId.unique().value()))
        );

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}
