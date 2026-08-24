package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.domain.DomainEvent;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.infrastructure.jpa.entities.EventEntity;
import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import br.com.fullcycle.infrastructure.jpa.repositories.EventJpaRepository;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Interface Adapter
@Component
public class EventDatabaseRepository implements EventRepository {

    private final EventJpaRepository eventJpaRepository;
    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper mapper;

    public EventDatabaseRepository(
            final EventJpaRepository EventJpaRepository,
            final OutboxJpaRepository outboxJpaRepository,
            final ObjectMapper mapper
    ) {
        this.eventJpaRepository = Objects.requireNonNull(EventJpaRepository);
        this.outboxJpaRepository = outboxJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Event> eventOfId(final EventId anId) {
        Objects.requireNonNull(anId, "id cannot be null");
        return this.eventJpaRepository.findById(UUID.fromString(anId.value()))
                .map(EventEntity::toEvent);
    }

    @Override
    @Transactional
    public Event create(final Event event) {
        return save(event);
    }

    @Override
    @Transactional
    public Event update(Event event) {
        return save(event);
    }

    @Override
    public void deleteAll() {
        this.eventJpaRepository.deleteAll();
    }

    private Event save(Event event) {
        this.outboxJpaRepository.saveAll(
                event.allDomainEvents().stream()
                        .map(it -> OutboxEntity.of(it, this::toJson))
                        .toList()
        );

        return this.eventJpaRepository.save(EventEntity.of(event))
                .toEvent();
    }

    private String toJson(DomainEvent domainEvent) {
        try {
            return this.mapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
