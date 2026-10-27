package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.models.Partner;
import br.com.fullcycle.hexagonal.services.PartnerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class CreatePartnerUseCaseTest {


    @Test
    @DisplayName("Deve criar um partner")
    public void testCreate() {

        // given
        final String expectedCNPJ = "41536538000100";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreatePartnerUseCase.Input(expectedCNPJ, expectedEmail, expectedName);

        // when
        final var PartnerService = Mockito.mock(PartnerService.class);
        Mockito.when(PartnerService.findByCnpj(expectedCNPJ)).thenReturn(Optional.empty());
        Mockito.when(PartnerService.findByEmail(expectedEmail)).thenReturn(Optional.empty());
        Mockito.when(PartnerService.save(any())).thenAnswer(a -> {
            var Partner = a.getArgument(0, Partner.class);
            Partner.setId(1L);
            return Partner;
        });

        final CreatePartnerUseCase useCase = new CreatePartnerUseCase(PartnerService);
        final CreatePartnerUseCase.Output output = useCase.execute(createInput);

        // then
        assertNotNull(output.id());
        assertEquals(expectedCNPJ, output.cnpj());
        assertEquals(expectedEmail, output.email());
        assertEquals(expectedName, output.name());
    }

    @Test
    @DisplayName("Não dece cadastrar um partner com CNPJ duplicado")
    public void testCreateWithDuplicatedCNPJShouldFail() {

        // given
        final String expectedCNPJ = "41536538000100";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreatePartnerUseCase.Input(expectedCNPJ, expectedEmail, expectedName);

        final var aPartner = new Partner();
        aPartner.setId(1L);
        aPartner.setCnpj(expectedCNPJ);
        aPartner.setEmail(expectedEmail);
        aPartner.setName(expectedName);

        // when
        final var PartnerService = Mockito.mock(PartnerService.class);
        Mockito.when(PartnerService.findByCnpj(expectedCNPJ)).thenReturn(Optional.of(aPartner));

        final CreatePartnerUseCase useCase = new CreatePartnerUseCase(PartnerService);

        // then
        assertThrows(ValidationException.class, () -> useCase.execute(createInput));
    }

    @Test
    @DisplayName("Não dece cadastrar um cnpj com email duplicado")
    public void testCreateWithDuplicatedEmailShouldFail() {

        // given
        final String expectedCNPJ = "41536538000100";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreatePartnerUseCase.Input(expectedCNPJ, expectedEmail, expectedName);

        final var aPartner = new Partner();
        aPartner.setId(1L);
        aPartner.setCnpj(expectedCNPJ);
        aPartner.setEmail(expectedEmail);
        aPartner.setName(expectedName);

        // when
        final var PartnerService = Mockito.mock(PartnerService.class);
        Mockito.when(PartnerService.findByEmail(expectedEmail)).thenReturn(Optional.of(aPartner));

        final CreatePartnerUseCase useCase = new CreatePartnerUseCase(PartnerService);

        // then
        assertThrows(ValidationException.class, () -> useCase.execute(createInput));
    }
}
