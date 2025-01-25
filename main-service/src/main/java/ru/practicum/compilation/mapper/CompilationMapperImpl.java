package ru.practicum.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.events.dto.EventShortDto;

import java.util.List;

@RequiredArgsConstructor
@Component
public final class CompilationMapperImpl {

    public CompilationDto toDto(Compilation compilation, List<EventShortDto> list) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.isPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(list);
        return dto;
    }


}
