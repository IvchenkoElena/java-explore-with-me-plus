package ru.practicum.events.service;

import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatClient;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.OperationForbiddenException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.dto.*;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.predicates.EventPredicates;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String MAIN_SERVICE = "ewm-main-service";

    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final StatClient statClient;

    @Override
    public List<EventDto> adminEventsSearch(List<Long> users, List<Long> categories, List<EventState> states, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        Predicate predicate = EventPredicates.adminFilter(users, categories, states, rangeStart, rangeEnd);
        if (predicate == null) {
            return eventRepository.findAll(pageable).stream().map(eventMapper::toDto).peek(this::addViewsAndConfirmedRequests).toList();
        } else {
            return eventRepository.findAll(predicate, pageable).stream().map(eventMapper::toDto).peek(this::addViewsAndConfirmedRequests).toList();
        }
    }

    @Override
    public EventDto adminEventUpdate(Long eventId, EventAdminUpdateDto eventUpdateDto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (eventUpdateDto.getEventDate() != null && eventUpdateDto.getEventDate().isBefore(event.getCreatedOn().minusHours(1))) {
            throw new ValidationException("Event date cannot be before created date");
        }

        updateEventData(event, eventUpdateDto.getTitle(),
                eventUpdateDto.getAnnotation(),
                eventUpdateDto.getDescription(),
                eventUpdateDto.getCategory(),
                eventUpdateDto.getEventDate(),
                eventUpdateDto.getLocation(),
                eventUpdateDto.getPaid(),
                eventUpdateDto.getRequestModeration(),
                eventUpdateDto.getParticipantLimit());
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
                event.setState(EventState.CANCELED);
            }
        }
        event = eventRepository.save(event);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    @Override
    public List<EventShortDto> getEvents(String text, EventSort sort, Integer from, Integer size, List<Long> categories, String rangeStart,
                                         String rangeEnd, Boolean paid, Boolean onlyAvailable, HttpServletRequest request) {

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (rangeStart != null && rangeEnd != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            start = LocalDateTime.parse(rangeStart, formatter);
            end = LocalDateTime.parse(rangeEnd, formatter);
            if (start.isAfter(end)) {
                throw new ValidationException("Start date can not be after end date");
            }
        }

        Pageable pageable = null;
        if (from != null && size != null) {
            pageable = PageRequest.of(from, size);
        }

        Predicate predicate = EventPredicates.publicFilter(text, categories, start, end, paid);

        List<Event> filteredEvents = new ArrayList<>();
        if (pageable != null && predicate != null) {
            filteredEvents = eventRepository.findAll(predicate, pageable).stream().toList();
        } else if (predicate != null) {
            Iterable<Event> iterableEvent = eventRepository.findAll(predicate);
            for (Event event : iterableEvent) {
                filteredEvents.add(event);
            }
        } else if (pageable != null) {
            filteredEvents = eventRepository.findAll(pageable).toList();
        }

        List<EventShortDto> available = filteredEvents
                .stream()
                .filter(this::isEventAvailable)
                .map(eventMapper::toEventShortDto)
                .peek(this::addViewsAndConfirmedRequests).toList();

        if (sort != null) {
            switch (sort) {
                case EVENT_DATE ->
                        available.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList();
                case VIEWS -> available.stream().sorted(Comparator.comparing(EventShortDto::getViews)).toList();
            }
        }

        saveHit(request);
        return available;

    }

    @Override
    public EventDto getEvent(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        saveHit(request);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }


    private void updateEventData(Event event, String title, String annotation, String description, Long categoryId, LocalDateTime eventDate, LocationDto location, Boolean paid, Boolean requestModeration, Integer participantLimit) {
        if (title != null) {
            event.setTitle(title);
        }
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException(String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s", eventDate));
            }
            event.setEventDate(eventDate);
        }
        if (location != null) {
            Location newLocation = locationRepository.save(locationMapper.toLocation(location));
            event.setLocation(newLocation);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
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
            throw new ValidationException(String
                    .format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            eventCreateDto.getEventDate()));
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
        updateEventData(event, eventUpdateDto.getTitle(),
                eventUpdateDto.getAnnotation(),
                eventUpdateDto.getDescription(),
                eventUpdateDto.getCategory(),
                eventUpdateDto.getEventDate(),
                eventUpdateDto.getLocation(),
                eventUpdateDto.getPaid(),
                eventUpdateDto.getRequestModeration(),
                eventUpdateDto.getParticipantLimit());
        if (eventUpdateDto.getStateAction() != null) {
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            }
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }
        event = eventRepository.save(event);
        return addViewsAndConfirmedRequests(eventMapper.toDto(event));
    }

    private EventDto addViewsAndConfirmedRequests(EventDto eventDto) {
        List<String> gettingUris = new ArrayList<>();
        gettingUris.add("/events/" + eventDto.getId());
        Long views = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now(), gettingUris, true)
                .stream().map(ViewStats::getHits).reduce(0L, Long::sum);
        eventDto.setViews(views);

        eventDto.setConfirmedRequests(requestRepository.countRequestsByEventAndStatus(eventRepository.findById(
                eventDto.getId()).get(), RequestStatus.CONFIRMED));

        return eventDto;
    }

    private EventShortDto addViewsAndConfirmedRequests(EventShortDto eventShortDto) {
        List<String> gettingUris = new ArrayList<>();
        gettingUris.add("/events/" + eventShortDto.getId());
        Long views = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now(), gettingUris, false)
                .stream().map(ViewStats::getHits).reduce(0L, Long::sum);
        eventShortDto.setViews(views);

        eventShortDto.setConfirmedRequests(requestRepository.countRequestsByEventAndStatus(eventRepository.findById(
                eventShortDto.getId()).get(), RequestStatus.CONFIRMED));

        return eventShortDto;
    }

    private boolean isEventAvailable(Event event) {
        Long confirmedRequestsAmount = requestRepository.countRequestsByEventAndStatus(event, RequestStatus.CONFIRMED);
        return event.getParticipantLimit() > confirmedRequestsAmount;
    }

    private void saveHit(HttpServletRequest request) {
        EndpointHitDto endpointHitDto = new EndpointHitDto();
        endpointHitDto.setApp(MAIN_SERVICE);
        endpointHitDto.setUri(request.getRequestURI());
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now());
        statClient.saveHit(endpointHitDto);
    }
}
