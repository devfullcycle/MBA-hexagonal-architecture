package br.com.fullcycle.hexagonal.infrastructure.jpa.entities;

import br.com.fullcycle.hexagonal.application.domain.event.Event;
import br.com.fullcycle.hexagonal.application.domain.event.EventTicket;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "Event")
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    private String name;

    private LocalDate date;

    private int totalSpots;

    private UUID partnerId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "event")
    private Set<EventTicketEntity> tickets;

    public EventEntity() {
        this.tickets = new HashSet<>();
    }

    public EventEntity(UUID id, String name, LocalDate date, int totalSpots, UUID partnerId) {
        this();
        this.id = id;
        this.name = name;
        this.date = date;
        this.totalSpots = totalSpots;
        this.partnerId = partnerId;
    }

    public static EventEntity of(final Event event) {
        final var entity = new EventEntity(
                UUID.fromString(event.eventId().value()),
                event.name().value(),
                event.date(),
                event.totalSpots(),
                UUID.fromString(event.partnerId().value())
        );

        event.allTickets().forEach(entity::addTicket);

        return entity;
    }

    public Event toEvent() {
        return Event.restore(
                this.id().toString(),
                this.name(),
                this.date().format(DateTimeFormatter.ISO_LOCAL_DATE),
                this.totalSpots(),
                this.partnerId().toString(),
                this.tickets().stream()
                        .map(EventTicketEntity::toEventTicket)
                        .collect(Collectors.toSet())
        );
    }

    private void addTicket(final EventTicket ticket) {
        this.tickets.add(EventTicketEntity.of(this, ticket));
    }

    public UUID id() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate date() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int totalSpots() {
        return totalSpots;
    }

    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
    }

    public UUID partnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public Set<EventTicketEntity> tickets() {
        return tickets;
    }

    public void setTickets(Set<EventTicketEntity> tickets) {
        this.tickets = tickets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventEntity event = (EventEntity) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
