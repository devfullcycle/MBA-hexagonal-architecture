package br.com.fullcycle.hexagonal.repositories;

import br.com.fullcycle.hexagonal.models.Event;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Long> {

}
