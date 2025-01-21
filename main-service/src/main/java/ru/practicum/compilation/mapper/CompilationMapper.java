package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;


@Mapper(componentModel = "spring")
public interface CompilationMapper {

    CompilationDto toDto(Compilation compilation);
}
