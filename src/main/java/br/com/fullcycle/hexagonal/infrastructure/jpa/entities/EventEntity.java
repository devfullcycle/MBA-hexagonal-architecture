package br.com.fullcycle.hexagonal.infrastructure.jpa.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String name;

    private LocalDate date;

    private int totalSpots;

    @ManyToOne(fetch = FetchType.LAZY)
    private PartnerEntity partner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event")
    private Set<TicketEntity> tickets;

    public EventEntity() {
        this.tickets = new HashSet<>();
    }

    public EventEntity(Long id, String name, LocalDate date, int totalSpots, Set<TicketEntity> tickets) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.totalSpots = totalSpots;
        this.tickets = tickets != null ? tickets : new HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTotalSpots() {
        return totalSpots;
    }

    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
    }

    public PartnerEntity getPartner() {
        return partner;
    }

    public void setPartner(PartnerEntity partner) {
        this.partner = partner;
    }

    public Set<TicketEntity> getTickets() {
        return tickets;
    }

    public void setTickets(Set<TicketEntity> tickets) {
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
