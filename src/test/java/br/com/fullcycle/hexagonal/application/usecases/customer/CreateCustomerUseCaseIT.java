package br.com.fullcycle.hexagonal.application.usecases.customer;

import br.com.fullcycle.hexagonal.IntegrationTest;
import br.com.fullcycle.hexagonal.application.domain.customer.Customer;
import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.application.repositories.CustomerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CreateCustomerUseCaseIT extends IntegrationTest {

    @Autowired
    private CreateCustomerUseCase useCase;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar um cliente")
    public void testCreateCustomer() {
        // given
        final var expectedCPF = "123.456.789-01";
        final var expectedEmail = "john.doe@gmail.com";
        final var expectedName = "John Doe";

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        // when
        final var output = useCase.execute(createInput);

        // then
        Assertions.assertNotNull(output.id());
        Assertions.assertEquals(expectedCPF, output.cpf());
        Assertions.assertEquals(expectedEmail, output.email());
        Assertions.assertEquals(expectedName, output.name());
    }

    @Test
    @DisplayName("Não deve cadastrar um cliente com CPF duplicado")
    public void testCreateWithDuplicatedCPFShouldFail() throws Exception {
        // given
        final var expectedCPF = "123.456.789-01";
        final var expectedEmail = "john.doe@gmail.com";
        final var expectedName = "John Doe";
        final var expectedError = "Customer already exists";

        createCustomer(expectedCPF, expectedEmail, expectedName);

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        // when
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(createInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cadastrar um cliente com e-mail duplicado")
    public void testCreateWithDuplicatedEmailShouldFail() throws Exception {
        // given
        final var expectedCPF = "123.456.789-01";
        final var expectedEmail = "john.doe@gmail.com";
        final var expectedName = "John Doe";
        final var expectedError = "Customer already exists";

        createCustomer("231.321.312-31", expectedEmail, expectedName);

        final var createInput = new CreateCustomerUseCase.Input(expectedCPF, expectedEmail, expectedName);

        // when
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(createInput));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    private Customer createCustomer(final String cpf, final String email, final String name) {
        return customerRepository.create(Customer.newCustomer(name, cpf, email));
    }
}
