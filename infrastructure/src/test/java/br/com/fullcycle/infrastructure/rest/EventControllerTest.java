package br.com.fullcycle.infrastructure.rest;

import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.application.event.CreateEventUseCase;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.infrastructure.dtos.NewEventDTO;
import br.com.fullcycle.infrastructure.dtos.SubscribeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class EventControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Customer johnDoe;
    private Partner disney;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        ticketRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();

        johnDoe = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-00", "john@gmail.com"));
        disney = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
    }

    @Test
    @DisplayName("Deve criar um evento")
    public void testCreate() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var result = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isString())
                .andReturn().getResponse().getContentAsByteArray();

        var actualResponse = mapper.readValue(result, CreateEventUseCase.Output.class);
        Assertions.assertEquals(event.date(), actualResponse.date());
        Assertions.assertEquals(event.totalSpots(), actualResponse.totalSpots());
        Assertions.assertEquals(event.name(), actualResponse.name());
        Assertions.assertEquals("ACTIVE", actualResponse.status());
    }

    @Test
    @Transactional
    @DisplayName("Deve comprar um ticket de um evento")
    public void testReserveTicket() throws Exception {

        var event = new NewEventDTO("Disney on Ice", "2021-01-01", 100, disney.partnerId().value());

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(event))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isString())
                .andReturn().getResponse().getContentAsByteArray();

        var eventId = mapper.readValue(createResult, CreateEventUseCase.Output.class).id();

        var sub = new SubscribeDTO(johnDoe.customerId().value(), null);

        this.mvc.perform(
                        MockMvcRequestBuilders.post("/events/{id}/subscribe", eventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(sub))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        var actualEvent = eventRepository.eventOfId(EventId.with(eventId)).get();
        Assertions.assertEquals(1, actualEvent.allTickets().size());
    }

    @Test
    @DisplayName("Deve cancelar um evento")
    public void testCancelEvent() throws Exception {
        // given
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));

        // when
        this.mvc.perform(MockMvcRequestBuilders.post("/events/{id}/cancel", event.eventId().value()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(event.eventId().value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CANCELLED"));

        // then
        var actualEvent = eventRepository.eventOfId(event.eventId()).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());
    }

    @Test
    @DisplayName("Deve consultar um evento por id com presenter completo")
    public void testGetEventById() throws Exception {
        // given
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));

        // when/then
        this.mvc.perform(MockMvcRequestBuilders.get("/events/{id}", event.eventId().value()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(event.eventId().value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Disney on Ice"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.date").value("2021-01-01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalSpots").value(100))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Deve consultar um evento por id com presenter pÃºblico")
    public void testGetEventByIdPublic() throws Exception {
        // given
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));

        // when/then
        this.mvc.perform(MockMvcRequestBuilders.get("/events/{id}", event.eventId().value())
                        .header("X-Public", "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(event.eventId().value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar 404 quando evento nÃ£o existe")
    public void testGetEventByIdNotFound() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get("/events/{id}", EventId.unique().value()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
