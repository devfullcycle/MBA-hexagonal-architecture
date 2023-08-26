package br.com.fullcycle.hexagonal.infrastructure.jpa.repositories;

import br.com.fullcycle.hexagonal.infrastructure.jpa.entities.EventEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface EventJpaRepository extends CrudRepository<EventEntity, UUID> {

}
