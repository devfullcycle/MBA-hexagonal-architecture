package br.com.fullcycle.hexagonal.infrastructure.dtos;

public class SubscribeDTO {

    private Long customerId;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}
