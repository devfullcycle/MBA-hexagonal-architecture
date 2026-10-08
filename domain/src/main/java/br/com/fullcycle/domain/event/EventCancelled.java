package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCancelled(
        String domainEventId,
        String type,
        String eventId,
        Instant occurredOn
) implements DomainEvent {

    public EventCancelled(EventId eventId) {
        this(UUID.randomUUID().toString(), "event.cancelled", eventId.value(), Instant.now());
    }
}
