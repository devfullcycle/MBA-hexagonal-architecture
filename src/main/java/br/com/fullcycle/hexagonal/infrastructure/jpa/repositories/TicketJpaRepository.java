package br.com.fullcycle.hexagonal.infrastructure.jpa.repositories;

import br.com.fullcycle.hexagonal.infrastructure.jpa.entities.TicketEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface TicketJpaRepository extends CrudRepository<TicketEntity, UUID> {

}
