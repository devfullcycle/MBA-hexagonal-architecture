package br.com.fullcycle.hexagonal.graphql;

import br.com.fullcycle.hexagonal.application.usecases.CreateCustomerUseCase;
import br.com.fullcycle.hexagonal.application.usecases.GetCustomerByIdUseCase;
import br.com.fullcycle.hexagonal.dtos.CustomerDTO;
import br.com.fullcycle.hexagonal.services.CustomerService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class CustomerResolver {

    private final CustomerService customerService;

    public CustomerResolver(CustomerService customerService) {
        this.customerService = customerService;
    }

    @MutationMapping
    public CreateCustomerUseCase.Output createCustomer(@Argument CustomerDTO dto) {
        final CreateCustomerUseCase useCase = new CreateCustomerUseCase(customerService);
        final CreateCustomerUseCase.Input input = new CreateCustomerUseCase.Input(
                dto.getCpf(),
                dto.getEmail(),
                dto.getName()
        );
        return useCase.execute(input);
    }

    @QueryMapping
    public GetCustomerByIdUseCase.Output customerOfId(@Argument Long id) {
        final var useCase = new GetCustomerByIdUseCase(customerService);
        return useCase.execute(new GetCustomerByIdUseCase.Input(id)).orElse(null);
    }
}
