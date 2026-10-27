package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.models.Customer;
import br.com.fullcycle.hexagonal.services.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetCustomerByIdUseCaseTest {


    @Test
    @DisplayName("Deve obter customer pelo id")
    public void testGetCustomerById() throws Exception {

        // given
        final Long expectedId = 1L;
        final String expectedCPF = "12345678901";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var aCustomer = new Customer();
        aCustomer.setId(1L);
        aCustomer.setCpf(expectedCPF);
        aCustomer.setEmail(expectedEmail);
        aCustomer.setName(expectedName);

        final var input = new GetCustomerByIdUseCase.Input(expectedId);

        // when
        final var customerService = Mockito.mock(CustomerService.class);
        Mockito.when(customerService.findById(expectedId)).thenReturn(Optional.of(aCustomer));

        final var useCase = new GetCustomerByIdUseCase(customerService);
        final GetCustomerByIdUseCase.Output output = useCase.execute(input).get();

        // then
        assertEquals(expectedId, output.id());
        assertEquals(expectedCPF, output.cpf());
        assertEquals(expectedEmail, output.email());
        assertEquals(expectedName, output.name());
    }

    @Test
    @DisplayName("Deve obter vazio ao tentar recuperar um cliente não existente por id")
    public void testGetCustomerByIdWithInvalidId() throws Exception {

        // given
        final Long expectedId = 1L;

        final var input = new GetCustomerByIdUseCase.Input(expectedId);

        // when
        final var customerService = Mockito.mock(CustomerService.class);
        Mockito.when(customerService.findById(expectedId)).thenReturn(Optional.empty());

        final var useCase = new GetCustomerByIdUseCase(customerService);
        final Optional<GetCustomerByIdUseCase.Output> output = useCase.execute(input);

        // then
        assertTrue(output.isEmpty());
    }
}