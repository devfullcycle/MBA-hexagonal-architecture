package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
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
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar um evento persistido")
    public void testCancelEvent() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var cancelInput = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals("CANCELLED", output.status());

        final var actualEvent = eventRepository.eventOfId(EventId.with(anEvent.eventId().value())).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEventThatDoesNotExist() throws Exception {
        // given
        final var expectedError = "Event not found";
        final var cancelInput = new CancelEventUseCase.Input(EventId.unique().value());

        // when
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(cancelInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}
