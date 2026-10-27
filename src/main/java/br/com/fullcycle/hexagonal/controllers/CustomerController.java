package br.com.fullcycle.hexagonal.controllers;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.application.usecases.CreateCustomerUseCase;
import br.com.fullcycle.hexagonal.application.usecases.GetCustomerByIdUseCase;
import br.com.fullcycle.hexagonal.dtos.CustomerDTO;
import br.com.fullcycle.hexagonal.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(value = "customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CustomerDTO dto) {
       try {
           final CreateCustomerUseCase useCase = new CreateCustomerUseCase(customerService);
           final CreateCustomerUseCase.Input input = new CreateCustomerUseCase.Input(
                   dto.getCpf(),
                   dto.getEmail(),
                   dto.getName()
           );
           final CreateCustomerUseCase.Output output = useCase.execute(input);

           return ResponseEntity.created(URI.create("/customer" + output.id())).body(output);
       } catch (ValidationException e) {
           return ResponseEntity.unprocessableEntity().body(e.getMessage());
       }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        final GetCustomerByIdUseCase useCase = new GetCustomerByIdUseCase(customerService);
        final GetCustomerByIdUseCase.Input input = new GetCustomerByIdUseCase.Input(id);

        return useCase.execute(input)
                .map(ResponseEntity::ok)
                .orElseGet(ResponseEntity.notFound()::build);
    }
}