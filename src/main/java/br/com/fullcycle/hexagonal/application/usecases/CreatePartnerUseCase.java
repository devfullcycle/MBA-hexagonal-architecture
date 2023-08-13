package br.com.fullcycle.hexagonal.application.usecases;

import br.com.fullcycle.hexagonal.application.UseCase;
import br.com.fullcycle.hexagonal.application.exceptions.ValidationException;
import br.com.fullcycle.hexagonal.infrastructure.models.Partner;
import br.com.fullcycle.hexagonal.infrastructure.services.PartnerService;

import java.util.Objects;

public class CreatePartnerUseCase extends UseCase<CreatePartnerUseCase.Input, CreatePartnerUseCase.Output> {

    private final PartnerService partnerService;

    public CreatePartnerUseCase(final PartnerService partnerService) {
        this.partnerService = Objects.requireNonNull(partnerService);
    }

    @Override
    public Output execute(final Input input) {
        if (partnerService.findByCnpj(input.cnpj).isPresent()) {
            throw new ValidationException("Partner already exists");
        }

        if (partnerService.findByEmail(input.email).isPresent()) {
            throw new ValidationException("Partner already exists");
        }

        var partner = new Partner();
        partner.setName(input.name);
        partner.setCnpj(input.cnpj);
        partner.setEmail(input.email);

        partner = partnerService.save(partner);

        return new Output(partner.getId(), partner.getCnpj(), partner.getEmail(), partner.getName());
    }

    public record Input(String cnpj, String email, String name) {
    }

    public record Output(Long id, String cnpj, String email, String name) {
    }
}
