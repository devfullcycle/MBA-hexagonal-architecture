package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.models.Partner;
import br.com.fullcycle.hexagonal.services.PartnerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPartnerByIdUseCaseTest {


    @Test
    @DisplayName("Deve obter partner pelo id")
    public void testGetPartnerById() {

        // given
        final Long expectedId = 1L;
        final String expectedCNPJ = "41536538000100";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var aPartner = new Partner();
        aPartner.setId(1L);
        aPartner.setCnpj(expectedCNPJ);
        aPartner.setEmail(expectedEmail);
        aPartner.setName(expectedName);

        final var input = new GetPartnerByIdUseCase.Input(expectedId);

        // when
        final var PartnerService = Mockito.mock(PartnerService.class);
        Mockito.when(PartnerService.findById(expectedId)).thenReturn(Optional.of(aPartner));

        final var useCase = new GetPartnerByIdUseCase(PartnerService);
        final GetPartnerByIdUseCase.Output output = useCase.execute(input).get();

        // then
        assertEquals(expectedId, output.id());
        assertEquals(expectedCNPJ, output.cnpj());
        assertEquals(expectedEmail, output.email());
        assertEquals(expectedName, output.name());
    }

    @Test
    @DisplayName("Deve obter vazio ao tentar recuperar um partner não existente por id")
    public void testGetPartnerByIdWithInvalidId() {

        // given
        final Long expectedId = 1L;

        final var input = new GetPartnerByIdUseCase.Input(expectedId);

        // when
        final var PartnerService = Mockito.mock(PartnerService.class);
        Mockito.when(PartnerService.findById(expectedId)).thenReturn(Optional.empty());

        final var useCase = new GetPartnerByIdUseCase(PartnerService);
        final Optional<GetPartnerByIdUseCase.Output> output = useCase.execute(input);

        // then
        assertTrue(output.isEmpty());
    }
}