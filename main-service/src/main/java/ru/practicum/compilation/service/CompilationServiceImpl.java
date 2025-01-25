package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatClient;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapperImpl;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapperImpl mapper;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestRepository requestRepository;
    private final StatClient statClient;


    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setPinned(dto.isPinned());
        compilation.setTitle(dto.getTitle());
        if (dto.getEvents() != null) {
            compilation.setEvents(dto.getEvents().stream().map(i -> eventRepository.findById(i)
                    .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", i)))).collect(Collectors.toSet()));
        }
        compilationRepository.save(compilation);
        List<EventShortDto> list = getEventShortDtoList(compilation);
        return mapper.toDto(compilation, list);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilationToDelete = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilationToUpdate = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        compilationToUpdate.setPinned(dto.isPinned());
        if (dto.getTitle() != null) {
            compilationToUpdate.setTitle(dto.getTitle());
        }
        if (dto.getEvents() != null) {
            compilationToUpdate.setEvents(dto.getEvents().stream().map(i -> eventRepository.findById(i)
                    .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", i)))).collect(Collectors.toSet()));
        }
        compilationRepository.save(compilationToUpdate);
        List<EventShortDto> list = getEventShortDtoList(compilationToUpdate);
        return mapper.toDto(compilationToUpdate, list);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from, size);
        if (pinned != null) {
            return compilationRepository.findByPinned(pinned, page).stream().map(c -> mapper.toDto(c, (getEventShortDtoList(c)))).toList();
        }
        return compilationRepository.findAll(page).stream().map(c -> mapper.toDto(c, (getEventShortDtoList(c)))).toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        List<EventShortDto> list = getEventShortDtoList(compilation);
        return mapper.toDto(compilation, list);
    }

    private List<EventShortDto> addViewsAndConfirmedRequestsToList(List<EventShortDto> eventDtoList) {

        List<Long> idsList = eventDtoList.stream().map(EventShortDto::getId).toList();
        List<Request> requests = requestRepository.findAllByEventIdIn(idsList);

        List<String> uris = eventDtoList.stream().map(dto -> "/events/" + dto.getId()).toList();
        List<ViewStats> viewStats = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now(), uris, false);

        List<EventShortDto> changedList = eventDtoList.stream()
                .peek(dto -> dto.setConfirmedRequests(requests.stream()
                        .filter(r -> Objects.equals(r.getEvent().getId(), dto.getId())).count()))
                .peek(dto -> dto.setViews(viewStats.stream()
                        .filter(v -> v.getUri().equals("/events/" + dto.getId()))
                        .map(ViewStats::getHits)
                        .reduce(0L, Long::sum)))
                .toList();

        return changedList;
    }

    private List<EventShortDto> getEventShortDtoList(Compilation compilation) {
        List<EventShortDto> list = addViewsAndConfirmedRequestsToList(compilation.getEvents().stream().map(eventMapper::toEventShortDto).toList());
        return list;
    }
}
