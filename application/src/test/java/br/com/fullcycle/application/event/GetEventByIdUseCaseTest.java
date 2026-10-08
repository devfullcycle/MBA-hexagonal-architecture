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
    @DisplayName("Deve obter um evento ativo por id")
    public void testGetActiveEventById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 100, aPartner);

        final var expectedId = anEvent.eventId().value();
        final var expectedName = "Disney on Ice";
        final var expectedDate = "2021-01-01";
        final var expectedTotalSpots = 100;
        final var expectedStatus = EventStatus.ACTIVE.name();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(expectedId));

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(expectedId, output.get().id());
        Assertions.assertEquals(expectedName, output.get().name());
        Assertions.assertEquals(expectedDate, output.get().date());
        Assertions.assertEquals(expectedTotalSpots, output.get().totalSpots());
        Assertions.assertEquals(expectedStatus, output.get().status());
    }

    @Test
    @DisplayName("Deve obter um evento cancelado com o status CANCELLED")
    public void testGetCancelledEventById() throws Exception {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 100, aPartner);
        anEvent.cancel();

        final var expectedStatus = EventStatus.CANCELLED.name();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertTrue(output.isPresent());
        Assertions.assertEquals(expectedStatus, output.get().status());
    }

    @Test
    @DisplayName("Deve retornar vazio ao obter um evento que não existe")
    public void testGetEventByIdWithoutEvent() throws Exception {
        // given
        final var eventRepository = new InMemoryEventRepository();

        // when
        final var useCase = new GetEventByIdUseCase(eventRepository);
        final var output = useCase.execute(new GetEventByIdUseCase.Input(EventId.unique().value()));

        // then
        Assertions.assertTrue(output.isEmpty());
    }
}
