package br.com.fullcycle.hexagonal.application.domain.person;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    @DisplayName("Deve instanciar um CPF valido")
    public void testCreateCPF() {
        // given
        final var expectedCPF = "411.536.538-00";

        // when
        final var actualCpf = new Cpf(expectedCPF);

        // then
        Assertions.assertEquals(expectedCPF, actualCpf.value());
    }

    @Test
    @DisplayName("Não deve instanciar um CPF invalido")
    public void testCreateCPFWithInvalidValue() {
        // given
        final var expectedError = "Invalid value for Cpf";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Cpf("123456.789-01")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve instanciar um CPF null")
    public void testCreateCPFWithNullValue() {
        // given
        final var expectedError = "Invalid value for Cpf";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> new Cpf(null)
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }
}