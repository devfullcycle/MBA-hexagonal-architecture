package br.com.fullcycle.hexagonal.application.domain.person;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CnpjTest {

    @Test
    @DisplayName("Deve instanciar um CNPJ valido")
    public void testCreateCNPJ() {
        // given
        final var expectedCNPJ = "41.536.538/0001-00";

        // when
        final var actualCnpj = new Cnpj(expectedCNPJ);

        // then
        Assertions.assertEquals(expectedCNPJ, actualCnpj.value());
    }

    @Test
    @DisplayName("Não deve instanciar um CNPJ invalido")
    public void testCreateCNPJWithInvalidValue() {
        // given
        final var expectedError = "Invalid value for Cnpj";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Cnpj("123456.789-01")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve instanciar um CNPJ null")
    public void testCreateCNPJWithNullValue() {
        // given
        final var expectedError = "Invalid value for Cnpj";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Cnpj(null)
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }
}