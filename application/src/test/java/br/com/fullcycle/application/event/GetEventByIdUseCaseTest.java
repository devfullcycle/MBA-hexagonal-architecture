package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEventByIdUseCaseTest {

    @Test
    @DisplayName("Deve obter um evento pelo id")
    public void testGetEventById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var getInput = new GetEventByIdUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(getInput);

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(anEvent.eventId().value(), output.get().id());
        Assertions.assertEquals("Disney on Ice", output.get().name());
        Assertions.assertEquals("2021-01-01", output.get().date());
        Assertions.assertEquals(10, output.get().totalSpots());
        Assertions.assertEquals("ACTIVE", output.get().status());
    }

    @Test
    @DisplayName("Não deve obter um evento que não existe")
    public void testGetEventByIdThatDoesNotExist() throws Exception {
        // given
        final var eventRepository = new InMemoryEventRepository();
        final var getInput = new GetEventByIdUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(getInput);

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
