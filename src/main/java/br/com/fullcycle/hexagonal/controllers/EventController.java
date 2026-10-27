package br.com.fullcycle.hexagonal.controllers;

import br.com.fullcycle.hexagonal.application.usecases.CreateEventUseCase;
import br.com.fullcycle.hexagonal.application.usecases.SubscribeCustomerToEventUseCase;
import br.com.fullcycle.hexagonal.dtos.EventDTO;
import br.com.fullcycle.hexagonal.dtos.SubscribeDTO;
import br.com.fullcycle.hexagonal.services.CustomerService;
import br.com.fullcycle.hexagonal.services.EventService;
import br.com.fullcycle.hexagonal.services.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping(value = "events")
public class EventController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EventService eventService;

    @Autowired
    private PartnerService partnerService;

    @PostMapping
    @ResponseStatus(CREATED)
    public ResponseEntity<?> create(@RequestBody EventDTO dto) {
        try {
            final Long partnerId = Objects
                    .requireNonNull(dto.getPartner(), "Partner is required").getId();

            final CreateEventUseCase.Input input = new CreateEventUseCase
                    .Input(dto.getDate(), dto.getName(), partnerId, dto.getTotalSpots());

            final CreateEventUseCase useCase =
                    new CreateEventUseCase(eventService, partnerService);

            final CreateEventUseCase.Output output = useCase.execute(input);
            return ResponseEntity.created(URI.create("/events/" + output.id())).body(output);
        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    @Transactional
    @PostMapping(value = "/{id}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable Long id, @RequestBody SubscribeDTO dto) {
        try {
            final SubscribeCustomerToEventUseCase.Input input =
                    new SubscribeCustomerToEventUseCase.Input(id, dto.getCustomerId());

            final SubscribeCustomerToEventUseCase useCase =
                    new SubscribeCustomerToEventUseCase(customerService, eventService);

            final SubscribeCustomerToEventUseCase.Output output = useCase.execute(input);

            return ResponseEntity.ok().body(output);
        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }
}
