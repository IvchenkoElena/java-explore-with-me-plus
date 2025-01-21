package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.ConfirmedRequests;

import java.util.Optional;

public interface ConfirmedRequestRepository extends JpaRepository<ConfirmedRequests, Long> {

    Optional<ConfirmedRequests> getConfirmedRequestsByEventId(Long eventId);

    Optional<ConfirmedRequests> findByEventId(Long eventId);

}
