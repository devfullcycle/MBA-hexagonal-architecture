package br.com.fullcycle.infrastructure.rest.presenters;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("publicGetEvent")
public class PublicGetEventByIdResponseEntity implements Presenter<Optional<GetEventByIdUseCase.Output>, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(PublicGetEventByIdResponseEntity.class);

    @Override
    public ResponseEntity<?> present(final Optional<GetEventByIdUseCase.Output> output) {
        return output
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(new Output(o.id(), o.status())))
                .orElseGet(ResponseEntity.notFound()::build);
    }

    @Override
    public ResponseEntity<?> present(Throwable error) {
        LOG.error("An error was observed at GetEventByIdUseCase", error);
        return ResponseEntity.notFound().build();
    }

    public record Output(String id, String status) {
    }
}
