package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.application.event.CreateEventUseCase;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import br.com.fullcycle.application.event.SubscribeCustomerToEventUseCase;
import br.com.fullcycle.infrastructure.dtos.NewEventDTO;
import br.com.fullcycle.infrastructure.dtos.SubscribeDTO;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Controller
public class EventResolver {

    private final CreateEventUseCase createEventUseCase;
    private final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase;
    private final CancelEventUseCase cancelEventUseCase;
    private final GetEventByIdUseCase getEventByIdUseCase;

    public EventResolver(
            final CreateEventUseCase createEventUseCase,
            final SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase,
            final CancelEventUseCase cancelEventUseCase,
            final GetEventByIdUseCase getEventByIdUseCase
    ) {
        this.createEventUseCase = Objects.requireNonNull(createEventUseCase);
        this.subscribeCustomerToEventUseCase = Objects.requireNonNull(subscribeCustomerToEventUseCase);
        this.cancelEventUseCase = Objects.requireNonNull(cancelEventUseCase);
        this.getEventByIdUseCase = Objects.requireNonNull(getEventByIdUseCase);
    }

    @MutationMapping
    public CreateEventUseCase.Output createEvent(@Argument NewEventDTO input) {
        return createEventUseCase.execute(new CreateEventUseCase.Input(input.date(), input.name(), input.partnerId(), input.totalSpots()));
    }

    @Transactional
    @MutationMapping
    public SubscribeCustomerToEventUseCase.Output subscribeCustomerToEvent(@Argument SubscribeDTO input) {
        return subscribeCustomerToEventUseCase.execute(new SubscribeCustomerToEventUseCase.Input(input.customerId(), input.eventId()));
    }

    @MutationMapping
    public CancelEventUseCase.Output cancelEvent(@Argument String id) {
        return cancelEventUseCase.execute(new CancelEventUseCase.Input(id));
    }

    @QueryMapping
    public GetEventByIdUseCase.Output eventOfId(@Argument String id) {
        return getEventByIdUseCase.execute(new GetEventByIdUseCase.Input(id)).orElse(null);
    }
}
