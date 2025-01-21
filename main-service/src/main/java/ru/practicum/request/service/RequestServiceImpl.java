package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.common.exception.*;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ConfirmedRequests;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ConfirmedRequestRepository;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final ConfirmedRequestRepository confirmedRequestRepository;

    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException(String.format("User with id %s not found",
                userId)));
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    //todo: refactoring
    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        if (eventRepository.findByIdAndInitiator_Id(eventId, userId).isPresent()) {
            throw new InitiatorRequestException(String.format("User with id %s is initiator for event with id %s",
                    userId, eventId));
        }
        if (!requestRepository.findByRequesterIdAndEventId(userId, eventId).isEmpty()) {
            throw new RepeatableUserRequestException(String.format("User with id %s already make request for event with id %s",
                    userId, eventId));
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(String.format(
                "Event with id %s not found", eventId)));
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotPublishEventException(String.format("Event with id %s is not published", eventId));
        }
        Request request = new Request();
        request.setRequester(userRepository.findById(userId).get());
        request.setEvent(event);
        if (event.isRequestModeration()) {
            Optional<ConfirmedRequests> confirmedRequests = confirmedRequestRepository.getConfirmedRequestsByEventId(eventId);
            if (confirmedRequests.isPresent()) {
                Long confirmedRequestsAmount = confirmedRequests.get().getConfirmedRequestsAmount();
                if (event.getParticipantLimit() >= confirmedRequestsAmount) {
                    throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
                }
            }
            request.setStatus(RequestStatus.PENDING);
            request.setCreatedOn(LocalDateTime.now());
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        } else {
            Optional<ConfirmedRequests> current = confirmedRequestRepository.findByEventId(eventId);
            if (current.isPresent()) {
                Long confirmedRequestsAmount = current.get().getConfirmedRequestsAmount();
                if (event.getParticipantLimit() >= confirmedRequestsAmount) {
                    throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
                }
            } else {
                request.setStatus(RequestStatus.CONFIRMED);
                request.setCreatedOn(LocalDateTime.now());
                ConfirmedRequests newConfirmRequest = new ConfirmedRequests();
                newConfirmRequest.setEvent(event);
                newConfirmRequest.setConfirmedRequestsAmount(1L);
                confirmedRequestRepository.save(newConfirmRequest);
            }
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        }
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request cancellingRequest = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id %s not found or unavailable " +
                        "for user with id %s", requestId, userId)));
        cancellingRequest.setStatus(RequestStatus.CANCELED);
        cancellingRequest = requestRepository.save(cancellingRequest);
        return requestMapper.requestToParticipationRequestDto(cancellingRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        List<Event> userEvents = eventRepository.findAllByInitiatorId(userId);
        Event event = userEvents.stream().filter(e -> e.getInitiator().getId().equals(userId)).findFirst()
                .orElseThrow(() -> new ValidationException(String.format("User with id %s is not initiator of event with id %s",
                        userId, eventId)));
        return requestRepository.findByEventId(event.getId())
                .stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    //todo: refactoring
    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest eventStatusUpdate) {
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found " +
                        "or unavailable for user with id %s", eventId, userId)));
        int participantLimit = event.getParticipantLimit();
        if (participantLimit == 0 || !event.isRequestModeration()) {
            throw new OperationUnnecessaryException(String.format("Requests confirm for event with id %s is not required",
                    eventId));
        }

        List<Long> requestIds = eventStatusUpdate.getRequestIds();
        List<Request> requests = requestIds.stream()
                .map(r -> requestRepository.findByIdAndEventId(r, eventId)
                        .orElseThrow(() -> new ValidationException(String.format("Request with id %s is not apply " +
                                "to user with id %s or event with id %s", r, userId, eventId))))
                .toList();

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        Optional<ConfirmedRequests> eventConfirmedRequest = confirmedRequestRepository.getConfirmedRequestsByEventId(eventId);
        Long confirmedRequestsAmount;
        if (eventConfirmedRequest.isPresent()) {
            confirmedRequestsAmount = eventConfirmedRequest.get().getConfirmedRequestsAmount();
        } else {
            ConfirmedRequests newConfirmedRequest = new ConfirmedRequests();
            newConfirmedRequest.setEvent(event);
            newConfirmedRequest.setConfirmedRequestsAmount(0L);
            confirmedRequestRepository.save(newConfirmedRequest);
            confirmedRequestsAmount = confirmedRequestRepository.getConfirmedRequestsByEventId(eventId).get().getConfirmedRequestsAmount();
        }
        if (confirmedRequestsAmount >= participantLimit) {
            throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
        }
        for (int i = 0; i < requests.size(); i++) {
            Request currentRequest = requests.get(i);
            if (currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                if (RequestStatus.from(eventStatusUpdate.getStatus()).equals(RequestStatus.CONFIRMED)) {
                    if (confirmedRequestsAmount <= participantLimit) {
                        currentRequest.setStatus(RequestStatus.CONFIRMED);
                        confirmedRequestsAmount++;
                        ConfirmedRequests current = confirmedRequestRepository.findByEventId(eventId).get();
                        current.setConfirmedRequestsAmount(confirmedRequestsAmount);
                        confirmedRequestRepository.save(current);
                        ParticipationRequestDto confirmed = requestMapper.requestToParticipationRequestDto(
                                requestRepository.save(currentRequest));
                        confirmedRequests.add(confirmed);
                    } else {
                        currentRequest.setStatus(RequestStatus.REJECTED);
                        ParticipationRequestDto rejected = requestMapper.requestToParticipationRequestDto(
                                requestRepository.save(currentRequest));
                        rejectedRequests.add(rejected);
                    }
                } else {
                    currentRequest.setStatus(RequestStatus.from(eventStatusUpdate.getStatus()));
                    ParticipationRequestDto rejected = requestMapper.requestToParticipationRequestDto(
                            requestRepository.save(currentRequest));
                    rejectedRequests.add(rejected);
                }
            }
        }
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);
        return result;
    }

}
