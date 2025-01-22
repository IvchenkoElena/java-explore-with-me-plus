package ru.practicum.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.client.StatClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class CompilationMapperUtil {
    private final EventMapper eventMapper;
    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;

    public List<EventShortDto> convert(Set<Event> events) {
        return events.stream().map(eventMapper::toEventShortDto).peek(this::addViewsAndConfirmedRequests).toList();
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
}
