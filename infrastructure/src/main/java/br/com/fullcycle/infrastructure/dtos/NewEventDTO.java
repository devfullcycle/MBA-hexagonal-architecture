package br.com.fullcycle.infrastructure.dtos;

public record NewEventDTO(
        String name,
        String date,
        Integer totalSpots,
        String partnerId
) {

}
