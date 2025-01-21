package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    Optional<Request> findByEventId(Long eventId);

    List<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<Request> findByIdAndEventId(Long id, Long eventId);

}
