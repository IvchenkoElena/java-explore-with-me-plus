package ru.practicum.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.OperationForbiddenException;
import ru.practicum.events.dto.*;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;

    @Override
    public List<EventDto> adminEventsSearch(List<Long> users, List<Long> categories, List<EventState> states, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        return List.of();
    }

    @Override
    public EventDto adminEventUpdate(Long eventId, EventAdminUpdateDto eventUpdateDto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (eventUpdateDto.getEventDate().isBefore(event.getCreatedOn().minusHours(1))) {
            throw new OperationForbiddenException("Event date cannot be before created date");
        }

        if (eventUpdateDto.getTitle() != null) {
            event.setTitle(eventUpdateDto.getTitle());
        }
        if (eventUpdateDto.getAnnotation() != null) {
            event.setAnnotation(eventUpdateDto.getAnnotation());
        }
        if (eventUpdateDto.getDescription() != null) {
            event.setDescription(eventUpdateDto.getDescription());
        }
        if (eventUpdateDto.getCategory() != null) {
            Category category = categoryRepository.findById(eventUpdateDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (eventUpdateDto.getEventDate() != null) {
            if (eventUpdateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new OperationForbiddenException(String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s", eventUpdateDto.getEventDate()));
            }
            event.setEventDate(eventUpdateDto.getEventDate());
        }
        if (eventUpdateDto.getLocation() != null) {
            Location newLocation = locationRepository.save(locationMapper.toLocation(eventUpdateDto.getLocation()));
            locationRepository.delete(event.getLocation());
            event.setLocation(newLocation);
        }
        if (eventUpdateDto.getPaid() != null) {
            event.setPaid(eventUpdateDto.getPaid());
        }
        if (eventUpdateDto.getRequestModeration() != null) {
            event.setRequestModeration(eventUpdateDto.getRequestModeration());
        }
        if (eventUpdateDto.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUpdateDto.getParticipantLimit());
        }
        if (eventUpdateDto.getStateAction() != null) {
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.PUBLISH_EVENT) && !event.getState().equals(EventState.PENDING)) {
                throw new OperationForbiddenException("Can't publish not pending event");
            }
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.REJECT_EVENT) && !event.getState().equals(EventState.PENDING)) {
                throw new OperationForbiddenException("Can't reject not pending event");
            }
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.PUBLISH_EVENT)) {
                event.setState(EventState.PUBLISHED);
            }
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.REJECT_EVENT)) {
                event.setState(EventState.CANCELLED);
            }
        }
        event = eventRepository.save(event);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    @Override
    public List<EventDto> privateUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        return eventRepository.findAllByInitiator_Id(userId, pageable).stream()
                .map(eventMapper::toDto)
                .peek(this::addViewsAndConfirmedRequests)
                .toList();
    }

    @Override
    public EventDto privateEventCreate(Long userId, EventCreateDto eventCreateDto) {
        if (eventCreateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new OperationForbiddenException(String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s", eventCreateDto.getEventDate()));
        }
        User initiator = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventMapper.fromDto(eventCreateDto);
        event.setInitiator(initiator);
        Category category = categoryRepository.findById(eventCreateDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        event.setCategory(category);
        locationRepository.save(event.getLocation());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    @Override
    public EventDto privateGetUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    @Override
    public EventDto privateUpdateUserEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        if (event.getState().equals(EventState.PUBLISHED) || event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new OperationForbiddenException("Only pending or canceled events can be changed");
        }
        if (eventUpdateDto.getTitle() != null) {
            event.setTitle(eventUpdateDto.getTitle());
        }
        if (eventUpdateDto.getAnnotation() != null) {
            event.setAnnotation(eventUpdateDto.getAnnotation());
        }
        if (eventUpdateDto.getDescription() != null) {
            event.setDescription(eventUpdateDto.getDescription());
        }
        if (eventUpdateDto.getCategory() != null) {
            Category category = categoryRepository.findById(eventUpdateDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (eventUpdateDto.getEventDate() != null) {
            if (eventUpdateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new OperationForbiddenException(String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s", eventUpdateDto.getEventDate()));
            }
            event.setEventDate(eventUpdateDto.getEventDate());
        }
        if (eventUpdateDto.getLocation() != null) {
            Location newLocation = locationRepository.save(locationMapper.toLocation(eventUpdateDto.getLocation()));
            locationRepository.delete(event.getLocation());
            event.setLocation(newLocation);
        }
        if (eventUpdateDto.getPaid() != null) {
            event.setPaid(eventUpdateDto.getPaid());
        }
        if (eventUpdateDto.getRequestModeration() != null) {
            event.setRequestModeration(eventUpdateDto.getRequestModeration());
        }
        if (eventUpdateDto.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUpdateDto.getParticipantLimit());
        }
        if (eventUpdateDto.getStateAction() != null) {
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            }
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELLED);
            }
        }
        event = eventRepository.save(event);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    private EventDto addViewsAndConfirmedRequests(EventDto eventDto) {
        //todo добавить загрузку views
        //todo добавить запрос к requests о количестве утвержденных
        return eventDto;
    }
}
