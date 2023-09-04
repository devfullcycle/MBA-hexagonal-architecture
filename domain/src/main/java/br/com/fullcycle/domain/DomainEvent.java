package br.com.fullcycle.domain;

import java.time.Instant;

public interface DomainEvent {

    String domainEventId();

    String type();

    Instant occurredOn();
}
