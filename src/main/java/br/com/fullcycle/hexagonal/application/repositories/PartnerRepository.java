package br.com.fullcycle.hexagonal.application.repositories;

import br.com.fullcycle.hexagonal.application.domain.partner.Partner;
import br.com.fullcycle.hexagonal.application.domain.partner.PartnerId;
import br.com.fullcycle.hexagonal.application.domain.person.Cnpj;
import br.com.fullcycle.hexagonal.application.domain.person.Email;

import java.util.Optional;

public interface PartnerRepository {

    Optional<Partner> partnerOfId(PartnerId anId);

    Optional<Partner> partnerOfCNPJ(Cnpj cnpj);

    Optional<Partner> partnerOfEmail(Email email);

    Partner create(Partner partner);

    Partner update(Partner partner);

    void deleteAll();
}
