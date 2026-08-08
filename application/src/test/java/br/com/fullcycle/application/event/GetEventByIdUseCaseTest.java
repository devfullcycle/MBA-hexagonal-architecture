package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEventByIdUseCaseTest {

    @Test
    @DisplayName("Deve obter um evento por id")
    public void testGetById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("Jão", "41.536.538/0001-00", "joao@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var eventID = anEvent.eventId().value();

        final var getInput = new GetEventByIdUseCase.Input(eventID);

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(getInput);

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(eventID, output.get().id());
        Assertions.assertEquals("Disney on Ice", output.get().name());
        Assertions.assertEquals("2021-01-01", output.get().date());
        Assertions.assertEquals(10, output.get().totalSpots());
        Assertions.assertEquals(EventStatus.ACTIVE.name(), output.get().status());
    }

    @Test
    @DisplayName("Não deve obter um evento que não existe")
    public void testGetByIdNotFound() throws Exception {
        // given
        final var getInput = new GetEventByIdUseCase.Input(EventId.unique().value());

        final var eventRepository = new InMemoryEventRepository();

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(getInput);

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
