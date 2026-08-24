package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.exceptions.ValidationException;

import java.util.UUID;

public record EventId(String value) {

    public EventId {
        if (value == null) {
            throw new ValidationException("Invalid value for EventId");
        }
    }

    public static EventId unique() {
        return new EventId(UUID.randomUUID().toString());
    }

    public static EventId with(final String value) {
        try {
            return new EventId(UUID.fromString(value).toString());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid value for EventId");
        }
    }
}
