package br.com.fullcycle.hexagonal.services;

import br.com.fullcycle.hexagonal.models.Partner;
import br.com.fullcycle.hexagonal.repositories.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PartnerService {

    @Autowired
    private PartnerRepository repository;

    @Transactional
    public Partner save(Partner customer) {
        return repository.save(customer);
    }

    public Optional<Partner> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Partner> findByCnpj(String cnpj) {
        return repository.findByCnpj(cnpj);
    }

    public Optional<Partner> findByEmail(String email) {
        return repository.findByEmail(email);
    }

}
