package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.model.Event;


@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "events", expression = "java(compilation.getEvents().stream().map(this::toEventShortDto).toList())")
    CompilationDto toDto(Compilation compilation);

    @Mapping(target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    EventShortDto toEventShortDto(Event event); // в предыдущем методе нужен был доступ к этому методу. не придумала никак, кроме как вынести этот метод прямо сюда
}
