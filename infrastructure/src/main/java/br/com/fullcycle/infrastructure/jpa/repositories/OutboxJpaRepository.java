package br.com.fullcycle.infrastructure.jpa.repositories;

import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends CrudRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findAllByPublishedFalse();
}
