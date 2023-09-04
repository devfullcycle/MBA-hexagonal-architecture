package br.com.fullcycle.infrastructure.rest;

import br.com.fullcycle.application.partner.CreatePartnerUseCase;
import br.com.fullcycle.application.partner.GetPartnerByIdUseCase;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.infrastructure.dtos.NewPartnerDTO;
import br.com.fullcycle.infrastructure.http.HttpRouter;
import br.com.fullcycle.infrastructure.http.HttpRouter.HttpRequest;
import br.com.fullcycle.infrastructure.http.HttpRouter.HttpResponse;

import java.net.URI;
import java.util.Objects;

// Adapter
public class PartnerFnController {

    private final CreatePartnerUseCase createPartnerUseCase;
    private final GetPartnerByIdUseCase getPartnerByIdUseCase;

    public PartnerFnController(
            final CreatePartnerUseCase createPartnerUseCase,
            final GetPartnerByIdUseCase getPartnerByIdUseCase
    ) {
        this.createPartnerUseCase = Objects.requireNonNull(createPartnerUseCase);
        this.getPartnerByIdUseCase = Objects.requireNonNull(getPartnerByIdUseCase);
    }

    public HttpRouter bind(final HttpRouter router) {
        router.GET("/partners/{id}", this::get);
        router.POST("/partners", this::create);
        return router;
    }

    private HttpResponse<?> create(final HttpRequest req) {
        try {
            final var dto = req.body(NewPartnerDTO.class);

            final var output =
                    createPartnerUseCase.execute(new CreatePartnerUseCase.Input(dto.cnpj(), dto.email(), dto.name()));

            return HttpResponse.created(URI.create("/partners/" + output.id())).body(output);
        } catch (ValidationException ex) {
            return HttpResponse.unprocessableEntity().body(ex.getMessage());
        }
    }

    private HttpResponse<?> get(final HttpRequest req) {
        final String id = req.pathParam("id");
        return getPartnerByIdUseCase.execute(new GetPartnerByIdUseCase.Input(id))
                .map(HttpResponse::ok)
                .orElseGet(HttpResponse.notFound()::build);
    }
}