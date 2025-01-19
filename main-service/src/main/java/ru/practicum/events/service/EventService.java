package ru.practicum.events.service;

import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventCreateDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventUpdateDto;
import ru.practicum.events.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    List<EventDto> adminEventsSearch(List<Long> users, List<Long> categories, List<EventState> states, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventDto adminEventUpdate(Long eventId, EventAdminUpdateDto eventDto);

    List<EventDto> privateUserEvents(Long userId, int from, int size);

    EventDto privateEventCreate(Long userId, EventCreateDto eventCreateDto);

    EventDto privateGetUserEvent(Long userId, Long eventId);

    EventDto privateUpdateUserEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto);

}
