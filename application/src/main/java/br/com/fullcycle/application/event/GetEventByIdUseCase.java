package br.com.fullcycle.application.event;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class GetEventByIdUseCase
        extends UseCase<GetEventByIdUseCase.Input, Optional<GetEventByIdUseCase.Output>> {

    private final EventRepository eventRepository;

    public GetEventByIdUseCase(final EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
    }

    @Override
    public Optional<Output> execute(final Input input) {
        return eventRepository.eventOfId(EventId.with(input.id()))
                .map(e -> new Output(
                        e.eventId().value(),
                        e.name().value(),
                        e.date().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        e.totalSpots(),
                        e.status().name())
                );
    }

    public record Input(String id) {
    }

    public record Output(String id, String name, String date, int totalSpots, String status) {
    }
}
