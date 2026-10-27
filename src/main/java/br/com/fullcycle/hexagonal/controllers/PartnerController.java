package br.com.fullcycle.hexagonal.controllers;

import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.application.usecases.CreatePartnerUseCase;
import br.com.fullcycle.hexagonal.application.usecases.GetPartnerByIdUseCase;
import br.com.fullcycle.hexagonal.dtos.PartnerDTO;
import br.com.fullcycle.hexagonal.services.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(value = "partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PartnerDTO dto) {
        try {
            final CreatePartnerUseCase useCase = new CreatePartnerUseCase(partnerService);
            final CreatePartnerUseCase.Input input = new CreatePartnerUseCase.Input(
                    dto.getCnpj(),
                    dto.getEmail(),
                    dto.getName()
            );
            final CreatePartnerUseCase.Output output = useCase.execute(input);

            return ResponseEntity.created(URI.create("/partners" + output.id())).body(output);
        } catch (ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        final GetPartnerByIdUseCase useCase = new GetPartnerByIdUseCase(partnerService);
        final GetPartnerByIdUseCase.Input input = new GetPartnerByIdUseCase.Input(id);

        return useCase.execute(input)
                .map(ResponseEntity::ok)
                .orElseGet(ResponseEntity.notFound()::build);
    }

}
