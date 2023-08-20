package br.com.fullcycle.hexagonal.infrastructure.dtos;

public record NewEventDTO(
        String name,
        String date,
        Integer totalSpots,
        String partnerId
) {

}
