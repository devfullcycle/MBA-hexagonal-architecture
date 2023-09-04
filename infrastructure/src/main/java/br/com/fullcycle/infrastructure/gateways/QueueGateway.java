package br.com.fullcycle.infrastructure.gateways;

public interface QueueGateway {
    void publish(String content);
}
