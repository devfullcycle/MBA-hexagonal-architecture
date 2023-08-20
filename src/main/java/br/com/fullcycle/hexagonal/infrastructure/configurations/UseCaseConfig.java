package br.com.fullcycle.hexagonal.infrastructure.configurations;

import br.com.fullcycle.hexagonal.application.usecases.*;
import br.com.fullcycle.hexagonal.infrastructure.services.CustomerService;
import br.com.fullcycle.hexagonal.infrastructure.services.EventService;
import br.com.fullcycle.hexagonal.infrastructure.services.PartnerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class UseCaseConfig {

    private final CustomerService customerService;
    private final EventService eventService;
    private final PartnerService partnerService;

    public UseCaseConfig(
            final CustomerService customerService,
            final EventService eventService,
            final PartnerService partnerService
    ) {
        this.customerService = Objects.requireNonNull(customerService);
        this.eventService = Objects.requireNonNull(eventService);
        this.partnerService = Objects.requireNonNull(partnerService);
    }

    @Bean
    public CreateCustomerUseCase createCustomerUseCase() {
        // TODO: Fix dependency
        return new CreateCustomerUseCase(null);
    }

    @Bean
    public CreateEventUseCase createEventUseCase() {
        // TODO: Fix dependency
        return new CreateEventUseCase(null, null);
    }

    @Bean
    public CreatePartnerUseCase createPartnerUseCase() {
        // TODO: Fix dependency
        return new CreatePartnerUseCase(null);
    }

    @Bean
    public GetCustomerByIdUseCase getCustomerByIdUseCase() {
        // TODO: Fix dependency
        return new GetCustomerByIdUseCase(null);
    }

    @Bean
    public GetPartnerByIdUseCase getPartnerByIdUseCase() {
        // TODO: Fix dependency
        return new GetPartnerByIdUseCase(null);
    }

    @Bean
    public SubscribeCustomerToEventUseCase subscribeCustomerToEventUseCase() {
        // TODO: Fix dependency
        return new SubscribeCustomerToEventUseCase(null, null, null);
    }
}
