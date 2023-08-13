package br.com.fullcycle.hexagonal.application.exceptions;

public class ValidationException extends RuntimeException {

    public ValidationException(final String message) {
        super(message, null, true, false);
    }

    public ValidationException(final String message, final Throwable cause) {
        super(message, cause, true, false);
    }
}
