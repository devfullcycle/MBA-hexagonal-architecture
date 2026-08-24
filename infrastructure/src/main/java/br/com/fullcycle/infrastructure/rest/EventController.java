package br.com.fullcycle.infrastructure.rest;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.application.event.CreateEventUseCase;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.application.event.SubscribeCustomerToEventUseCase;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.infrastructure.dtos.NewEventDTO;
import br.com.fullcycle.infrastructure.dtos.SubscribeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

// Adapter
@RestController
@RequestMapping(value = "events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase;
    private final CancelEventUseCase cancelEventUseCase;
    private final GetEventByIdUseCase getEventByIdUseCase;
    private final Presenter<Optional<GetEventByIdUseCase.Output>, Object> privateGetEventPresenter;
    private final Presenter<Optional<GetEventByIdUseCase.Output>, Object> publicGetEventPresenter;

    public EventController(
            final CreateEventUseCase createEventUseCase,
            final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase,
            final CancelEventUseCase cancelEventUseCase,
            final GetEventByIdUseCase getEventByIdUseCase,
            final Presenter<Optional<GetEventByIdUseCase.Output>, Object> privateGetEvent,
            final Presenter<Optional<GetEventByIdUseCase.Output>, Object> publicGetEvent
    ) {
        this.createEventUseCase = Objects.requireNonNull(createEventUseCase);
        this.subscribeCustomerToEventUseCase = Objects.requireNonNull(subscribeCustomerToEventUseCase);
        this.cancelEventUseCase = Objects.requireNonNull(cancelEventUseCase);
        this.getEventByIdUseCase = Objects.requireNonNull(getEventByIdUseCase);
        this.privateGetEventPresenter = Objects.requireNonNull(privateGetEvent);
        this.publicGetEventPresenter = Objects.requireNonNull(publicGetEvent);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> create(@RequestBody NewEventDTO dto) {
        try {
            final var output =
                    createEventUseCase.execute(new CreateEventUseCase.Input(dto.date(), dto.name(), dto.partnerId(), dto.totalSpots()));

            return ResponseEntity.created(URI.create("/events/" + output.id())).body(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/{id}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable String id, @RequestBody SubscribeDTO dto) {
        try {
            final var output =
                    subscribeCustomerToEventUseCase.execute(new SubscribeCustomerToEventUseCase.Input(dto.customerId(), id));

            return ResponseEntity.ok(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @PostMapping(value = "/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        try {
            final var output = cancelEventUseCase.execute(new CancelEventUseCase.Input(id));

            return ResponseEntity.ok(output);
        } catch (ValidationException ex) {
            return ResponseEntity.unprocessableEntity().body(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable String id, @RequestHeader(name = "X-Public", required = false) String xPublic) {
        Presenter<Optional<GetEventByIdUseCase.Output>, Object> presenter = privateGetEventPresenter;

        if (xPublic != null) {
            presenter = publicGetEventPresenter;
        }

        return getEventByIdUseCase.execute(new GetEventByIdUseCase.Input(id), presenter);
    }
}
