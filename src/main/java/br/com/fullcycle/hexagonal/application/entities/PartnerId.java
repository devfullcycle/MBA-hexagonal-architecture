package br.com.fullcycle.hexagonal.application.entities;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;

import java.util.UUID;

public record PartnerId(UUID value) {

    public PartnerId {
        if (value == null) {
            throw new ValidationException("Invalid value for CustomerId");
        }
    }

    public static PartnerId unique() {
        return new PartnerId(UUID.randomUUID());
    }

    public static PartnerId with(final String value) {
        try {
            return new PartnerId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid value for PartnerId");
        }
    }
}
