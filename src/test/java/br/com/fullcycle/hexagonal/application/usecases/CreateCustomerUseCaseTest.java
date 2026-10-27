package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.models.Customer;
import br.com.fullcycle.hexagonal.services.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class CreateCustomerUseCaseTest {


    @Test
    @DisplayName("Deve criar um cliente")
    public void testCreate() {

        // given
        final String expectedCPF = "12345678901";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        // when
        final var customerService = Mockito.mock(CustomerService.class);
        Mockito.when(customerService.findByCpf(expectedCPF)).thenReturn(Optional.empty());
        Mockito.when(customerService.findByEmail(expectedEmail)).thenReturn(Optional.empty());
        Mockito.when(customerService.save(any())).thenAnswer(a -> {
            var customer = a.getArgument(0, Customer.class);
            customer.setId(1L);
            return customer;
        });

        final CreateCustomerUseCase useCase = new CreateCustomerUseCase(customerService);
        final CreateCustomerUseCase.Output output = useCase.execute(createInput);

        // then
        assertNotNull(output.id());
        assertEquals(expectedCPF, output.cpf());
        assertEquals(expectedEmail, output.email());
        assertEquals(expectedName, output.name());
    }

    @Test
    @DisplayName("Não dece cadastrar um cliente com CPF duplicado")
    public void testCreateWithDuplicatedCPFShouldFail() throws Exception {

        // given
        final String expectedCPF = "12345678901";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        final var aCustomer = new Customer();
        aCustomer.setId(1L);
        aCustomer.setCpf(expectedCPF);
        aCustomer.setEmail(expectedEmail);
        aCustomer.setName(expectedName);

        // when
        final var customerService = Mockito.mock(CustomerService.class);
        Mockito.when(customerService.findByCpf(expectedCPF)).thenReturn(Optional.of(aCustomer));

        final CreateCustomerUseCase useCase = new CreateCustomerUseCase(customerService);

        // then
        assertThrows(ValidationException.class, () -> useCase.execute(createInput));
    }

    @Test
    @DisplayName("Não dece cadastrar um cliente com email duplicado")
    public void testCreateWithDuplicatedEmailShouldFail() throws Exception {

        // given
        final String expectedCPF = "12345678901";
        final String expectedEmail = "john.doe@gmail.com";
        final String expectedName = "John Doe";

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        final var aCustomer = new Customer();
        aCustomer.setId(1L);
        aCustomer.setCpf(expectedCPF);
        aCustomer.setEmail(expectedEmail);
        aCustomer.setName(expectedName);

        // when
        final var customerService = Mockito.mock(CustomerService.class);
        Mockito.when(customerService.findByEmail(expectedEmail)).thenReturn(Optional.of(aCustomer));

        final CreateCustomerUseCase useCase = new CreateCustomerUseCase(customerService);

        // then
        assertThrows(ValidationException.class, () -> useCase.execute(createInput));
    }
}
