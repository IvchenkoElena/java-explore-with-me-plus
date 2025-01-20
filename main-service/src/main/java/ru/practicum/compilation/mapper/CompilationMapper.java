package ru.practicum.compilation.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.repository.EventRepository;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompilationMapper {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public Compilation toCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setPinned(dto.isPinned());
        compilation.setTitle(dto.getTitle());
        if (dto.getEvents() != null) {
            compilation.setEvents(dto.getEvents().stream().map(i -> eventRepository.findById(i)
                    .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", i)))).collect(Collectors.toSet()));
        }
        return compilation;
    }

    public CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.isPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(compilation.getEvents().stream().map(eventMapper::toEventShortDto).toList());
        return dto;
    }
}
