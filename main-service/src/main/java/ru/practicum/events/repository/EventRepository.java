package ru.practicum.events.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.events.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByInitiator_Id(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiator_Id(Long eventId, Long userId);

    List<Event> findAllByCategoryId(Long categoryId);
}
