package br.com.fullcycle.domain.partner;

import br.com.fullcycle.domain.person.Cnpj;
import br.com.fullcycle.domain.person.Email;

import java.util.Optional;

public interface PartnerRepository {

    Optional<Partner> partnerOfId(PartnerId anId);

    Optional<Partner> partnerOfCNPJ(Cnpj cnpj);

    Optional<Partner> partnerOfEmail(Email email);

    Partner create(Partner partner);

    Partner update(Partner partner);

    void deleteAll();
}
