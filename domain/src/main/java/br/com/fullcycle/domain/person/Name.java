package br.com.fullcycle.domain.person;

import br.com.fullcycle.domain.exceptions.ValidationException;

public record Name(String value) {

    public Name {
        if (value == null) {
            throw new ValidationException("Invalid value for Name");
        }
    }
}
