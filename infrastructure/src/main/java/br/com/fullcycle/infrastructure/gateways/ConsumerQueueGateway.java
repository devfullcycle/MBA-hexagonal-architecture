package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.application.ticket.CancelEventTicketsUseCase;
import br.com.fullcycle.application.ticket.CreateTicketForCustomerUseCase;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventTicketReserved;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ConsumerQueueGateway implements QueueGateway {

    private final CreateTicketForCustomerUseCase createTicketForCustomerUseCase;
    private final CancelEventTicketsUseCase cancelEventTicketsUseCase;
    private final ObjectMapper mapper;

    public ConsumerQueueGateway(
            final CreateTicketForCustomerUseCase createTicketForCustomerUseCase,
            final CancelEventTicketsUseCase cancelEventTicketsUseCase,
            final ObjectMapper mapper
    ) {
        this.createTicketForCustomerUseCase = Objects.requireNonNull(createTicketForCustomerUseCase);
        this.cancelEventTicketsUseCase = Objects.requireNonNull(cancelEventTicketsUseCase);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Async(value = "queueExecutor")
    @Override
    public void publish(final String content) {
        if (content == null) {
            return;
        }

        if (content.contains("event-ticket.reserved")) {
            final var dto = safeRead(content, EventTicketReserved.class);
            this.createTicketForCustomerUseCase.execute(new CreateTicketForCustomerUseCase.Input(dto.eventTicketId(), dto.eventId(), dto.customerId()));
        } else if (content.contains("event.cancelled")) {
            final var dto = safeRead(content, EventCancelled.class);
            this.cancelEventTicketsUseCase.execute(new CancelEventTicketsUseCase.Input(dto.eventId()));
        }
    }

    private <T> T safeRead(final String content, final Class<T> tClass) {
        try {
            return this.mapper.readValue(content, tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
