package br.com.fullcycle.hexagonal.application.domain.person;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    @DisplayName("Deve instanciar um Email valido")
    public void testCreateEmail() {
        // given
        final var expectedEmail = "john@gmail.com";

        // when
        final var actualEmail = new Email(expectedEmail);

        // then
        Assertions.assertEquals(expectedEmail, actualEmail.value());
    }

    @Test
    @DisplayName("Não deve instanciar um Email invalido")
    public void testCreateEmailWithInvalidValue() {
        // given
        final var expectedError = "Invalid value for Email";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Email("josh@s")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve instanciar um Email null")
    public void testCreateEmailWithNullValue() {
        // given
        final var expectedError = "Invalid value for Email";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Email(null)
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }
}