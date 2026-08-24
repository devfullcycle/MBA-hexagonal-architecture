package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

class ConsumerQueueGatewayCancelEventIT extends IntegrationTest {

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar todos os tickets do evento ao processar um EventCancelled recebido pela fila")
    public void testCascadeCancelsTicketsWhenEventCancelledIsConsumedFromQueue() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        final var aCustomer = customerRepository.create(Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com"));
        final var aCustomer2 = customerRepository.create(Customer.newCustomer("Pedro Doe", "123.111.789-01", "pedro.doe@gmail.com"));

        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(aCustomer2.customerId(), anEvent.eventId()));

        final var eventCancelled = new EventCancelled(anEvent.eventId());
        final var json = mapper.writeValueAsString(eventCancelled);

        // when: dispara o EventCancelled pelo caminho real do gateway, o mesmo usado pelo OutboxRelay
        consumerQueueGateway.publish(json);

        // then: publish é assíncrono, então aguarda o processamento com timeout
        final var deadline = Instant.now().plus(Duration.ofSeconds(5));
        List<Ticket> ticketsOfEvent;
        do {
            Thread.sleep(100);
            ticketsOfEvent = ticketRepository.ticketsByEventId(anEvent.eventId());
        } while (Instant.now().isBefore(deadline)
                && ticketsOfEvent.stream().anyMatch(it -> it.status() != TicketStatus.CANCELLED));

        Assertions.assertEquals(2, ticketsOfEvent.size());
        Assertions.assertTrue(ticketsOfEvent.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED));
    }
}
