package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.events.model.Event;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "confirmed_requests")
public class ConfirmedRequests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "requests_amount")
    private Long confirmedRequestsAmount;

}
