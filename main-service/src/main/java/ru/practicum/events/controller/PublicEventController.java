package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;


    /**
     * Получение событий с возможностью фильтрации.
     * В выдаче - только опубликованные события.
     * Текстовый поиск (по аннотации и подробному описанию) - без учета регистра букв.
     * Если в запросе не указан диапазон дат [rangeStart-rangeEnd], то выгружаются события,
     * которые происходят позже текущей даты и времени.
     * Информация о каждом событии включает в себя количество просмотров и количество уже одобренных заявок на участие.
     * Информация о том, что по эндпоинту был осуществлен и обработан запрос, сохраняется в сервисе статистики.
     * В случае, если по заданным фильтрам не найдено ни одного события, возвращается пустой список.
     *
     * @param text          текст для поиска в содержимом аннотации и подробном описании события
     * @param sort          Вариант сортировки: по дате события или по количеству просмотров
     * @param from          количество событий, которые нужно пропустить для формирования текущего набора
     * @param size          количество событий в наборе
     * @param categories    список идентификаторов категорий в которых будет вестись поиск
     * @param rangeStart    дата и время не раньше которых должно произойти событие
     * @param rangeEnd      дата и время не позже которых должно произойти событие
     * @param paid          поиск только платных/бесплатных событий
     * @param onlyAvailable только события у которых не исчерпан лимит запросов на участие
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    //400 - Запрос составлен некорректно ApiError
    public List<EventShortDto> getEvents(@RequestParam(required = false) @Size(min = 1, max = 7000) String text,
                                         @RequestParam(required = false) EventSort sort,
                                         @RequestParam(required = false, defaultValue = "0") @Min(0) Integer from,
                                         @RequestParam(required = false, defaultValue = "10") Integer size,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) String rangeStart,
                                         @RequestParam(required = false) String rangeEnd,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false) Boolean onlyAvailable,
                                         HttpServletRequest request) {
        return eventService.getEvents(text, sort, from, size, categories, rangeStart, rangeEnd, paid, onlyAvailable, request);
    }

    /**
     * Получение подробной информации об опубликованном событии по его идентификатору.
     * Cобытие должно быть опубликовано.
     * Информация о событии должна включать в себя количество просмотров и количество подтвержденных запросов.
     * Информация о том, что по эндпоинту был осуществлен и обработан запрос, сохраняется в сервисе статистики.
     *
     * @param id id события
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    //400 - Запрос составлен некорректно ApiError
    //404 - Событие не найдено или недоступно ApiError
    public EventDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        return eventService.getEvent(id, request);
    }

    /*
каждое событие должно относиться к какой-то из закреплённых в приложении категорий;
должна быть настроена возможность получения всех имеющихся категорий и подборок событий
(такие подборки будут составлять администраторы ресурса);
     */


}
