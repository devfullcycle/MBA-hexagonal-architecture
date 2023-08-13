package br.com.fullcycle.hexagonal.infrastructure.dtos;

import br.com.fullcycle.hexagonal.infrastructure.models.Ticket;
import br.com.fullcycle.hexagonal.infrastructure.models.TicketStatus;

import java.time.Instant;

public class TicketDTO {
    private Long id;
    private int spot;
    private CustomerDTO customer;
    private EventDTO event;
    private TicketStatus status;
    private Instant paidAt;
    private Instant reservedAt;

    public TicketDTO() {
    }

    public TicketDTO(Ticket ticket) {
        this.id = ticket.getId();
        this.customer = new CustomerDTO(ticket.getCustomer());
        this.event = new EventDTO(ticket.getEvent());
        this.status = ticket.getStatus();
        this.paidAt = ticket.getPaidAt();
        this.reservedAt = ticket.getReservedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSpot() {
        return spot;
    }

    public void setSpot(int spot) {
        this.spot = spot;
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    public EventDTO getEvent() {
        return event;
    }

    public void setEvent(EventDTO event) {
        this.event = event;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }
}
