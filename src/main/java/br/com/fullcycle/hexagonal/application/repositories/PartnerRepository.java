package br.com.fullcycle.hexagonal.application.repositories;

import br.com.fullcycle.hexagonal.application.entities.Partner;
import br.com.fullcycle.hexagonal.application.entities.PartnerId;

import java.util.Optional;

public interface PartnerRepository {

    Optional<Partner> partnerOfId(PartnerId anId);

    Optional<Partner> partnerOfCNPJ(String cpf);

    Optional<Partner> partnerOfEmail(String email);

    Partner create(Partner partner);

    Partner update(Partner partner);

}
