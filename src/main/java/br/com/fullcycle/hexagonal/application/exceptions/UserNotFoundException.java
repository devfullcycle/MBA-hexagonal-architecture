package br.com.fullcycle.hexagonal.application.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message, null, true, false);
    }
}
