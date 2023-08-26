package br.com.fullcycle.hexagonal.application.repository;

import br.com.fullcycle.hexagonal.application.domain.event.Event;
import br.com.fullcycle.hexagonal.application.domain.event.EventId;
import br.com.fullcycle.hexagonal.application.repositories.EventRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryEventRepository implements EventRepository {

    private final Map<String, Event> events;

    public InMemoryEventRepository() {
        this.events = new HashMap<>();
    }

    @Override
    public Optional<Event> eventOfId(EventId anId) {
        return Optional.ofNullable(this.events.get(Objects.requireNonNull(anId).value()));
    }

    @Override
    public Event create(Event event) {
        this.events.put(event.eventId().value(), event);
        return event;
    }

    @Override
    public Event update(Event event) {
        this.events.put(event.eventId().value(), event);
        return event;
    }

    @Override
    public void deleteAll() {
        this.events.clear();
    }
}
