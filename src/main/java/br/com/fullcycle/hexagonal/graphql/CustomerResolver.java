package br.com.fullcycle.hexagonal.graphql;

import br.com.fullcycle.hexagonal.dtos.CustomerDTO;
import br.com.fullcycle.hexagonal.models.Customer;
import br.com.fullcycle.hexagonal.services.CustomerService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Objects;

@Controller
public class CustomerResolver {

    private final CustomerService customerService;

    public CustomerResolver(final CustomerService customerService) {
        this.customerService = Objects.requireNonNull(customerService);
    }

    @MutationMapping
    public CustomerDTO createCustomer(@Argument CustomerDTO input) {
        if (customerService.findByCpf(input.getCpf()).isPresent()) {
            throw new RuntimeException("Customer already exists");
        }
        if (customerService.findByEmail(input.getEmail()).isPresent()) {
            throw new RuntimeException("Customer already exists");
        }

        var customer = new Customer();
        customer.setName(input.getName());
        customer.setCpf(input.getCpf());
        customer.setEmail(input.getEmail());

        customer = customerService.save(customer);

        return new CustomerDTO(customer);
    }

    @QueryMapping
    public CustomerDTO customerOfId(@Argument Long id) {
        return customerService.findById(id)
                .map(CustomerDTO::new)
                .orElse(null);
    }
}
