package ru.practicum.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;

@RequiredArgsConstructor
@Component
public final class CompilationMapperImpl {
    private final CompilationMapperUtil compilationMapperUtil;

    public CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.isPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(compilationMapperUtil.convert(compilation.getEvents()));
        return dto;
    }


}
