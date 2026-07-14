package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventUseCaseTest {

    @Test
    @DisplayName("Deve cancelar um evento")
    public void testCancelEvent() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var output = useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.id());
        Assertions.assertEquals(EventStatus.CANCELLED.name(), output.status());
        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());
        Assertions.assertEquals("event.cancelled", actualEvent.allDomainEvents().iterator().next().type());
    }

    @Test
    @DisplayName("NÃ£o deve cancelar um evento inexistente")
    public void testCancelEventWhenNotFound() {
        // given
        final var eventRepository = new InMemoryEventRepository();
        final var expectedError = "Event not found";

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> useCase.execute(new CancelEventUseCase.Input(EventId.unique().value()))
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("NÃ£o deve cancelar um evento jÃ¡ cancelado")
    public void testCancelEventTwice() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var eventRepository = new InMemoryEventRepository();
        final var expectedError = "Event already cancelled";
        anEvent.cancel();
        eventRepository.create(anEvent);

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()))
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }
}
