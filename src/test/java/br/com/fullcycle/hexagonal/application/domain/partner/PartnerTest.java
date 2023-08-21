package br.com.fullcycle.hexagonal.application.domain.partner;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PartnerTest {

    @Test
    @DisplayName("Deve instanciar um partner")
    public void testCreatePartner() {
        // given
        final var expectedCNPJ = "41.536.538/0001-00";
        final var expectedEmail = "john.doe@gmail.com";
        final var expectedName = "John Doe";

        // when
        final var actualPartner = Partner.newPartner(expectedName, expectedCNPJ, expectedEmail);

        // then
        Assertions.assertNotNull(actualPartner.partnerId());
        Assertions.assertEquals(expectedCNPJ, actualPartner.cnpj().value());
        Assertions.assertEquals(expectedEmail, actualPartner.email().value());
        Assertions.assertEquals(expectedName, actualPartner.name().value());
    }

    @Test
    @DisplayName("Não deve instanciar um partner com CNPJ invalido")
    public void testCreatePartnerWithInvalidCNPJ() {
        // given
        final var expectedError = "Invalid value for Cnpj";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> Partner.newPartner("John Doe", "123456.789-01", "john.doe@gmail.com")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }

    @Test
    @DisplayName("Não deve instanciar um partner com nome invalido")
    public void testCreatePartnerWithInvalidName() {
        // given
        final var expectedError = "Invalid value for Name";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> Partner.newPartner(null, "41.536.538/0001-00", "john.doe@gmail.com")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }


    @Test
    @DisplayName("Não deve instanciar um partner com email invalido")
    public void testCreatePartnerWithInvalidEmail() {
        // given
        final var expectedError = "Invalid value for Email";

        // when
        final var actualError = Assertions.assertThrows(
                ValidationException.class,
                () -> Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail")
        );

        // then
        Assertions.assertEquals(expectedError, actualError.getMessage());
    }
}
