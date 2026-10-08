package br.com.fullcycle.application.event;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.exceptions.ValidationException;

import java.util.Objects;

public class CancelEventUseCase extends UseCase<CancelEventUseCase.Input, CancelEventUseCase.Output> {

    private final EventRepository eventRepository;

    public CancelEventUseCase(final EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
    }

    @Override
    public Output execute(final Input input) {
        final var anEvent = eventRepository.eventOfId(EventId.with(input.eventId()))
                .orElseThrow(() -> new ValidationException("Event not found"));

        anEvent.cancel();

        eventRepository.update(anEvent);

        return new Output(anEvent.eventId().value(), anEvent.status().name());
    }

    public record Input(String eventId) {
    }

    public record Output(String id, String status) {
    }
}
