package ru.practicum.events.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.events.dto.*;
import ru.practicum.events.model.EventSort;
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

    List<EventShortDto> getEvents(String text, EventSort sort, Integer from, Integer size, List<Long> categories, String rangeStart,
                                  String rangeEnd, Boolean paid, Boolean onlyAvailable, HttpServletRequest request);

    EventDto getEvent(Long eventId, HttpServletRequest request);

}
