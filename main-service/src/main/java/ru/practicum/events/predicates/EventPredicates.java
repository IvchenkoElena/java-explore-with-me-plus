package ru.practicum.events.predicates;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.QEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EventPredicates {

    private EventPredicates() {
    }

    private static BooleanExpression initiatorIdIn(List<Long> uIds) {
        return QEvent.event.initiator.id.in(uIds);
    }

    private static BooleanExpression statesIn(List<EventState> states) {
        return QEvent.event.state.in(states);
    }

    private static BooleanExpression isPublish() {
        return QEvent.event.state.eq(EventState.PUBLISHED);
    }

    private static BooleanExpression categoriesIn(List<Long> cIds) {
        return QEvent.event.category.id.in(cIds);
    }

    private static BooleanExpression eventDateGoe(LocalDateTime from) {
        return QEvent.event.eventDate.goe(from);
    }

    private static BooleanExpression eventDateLoe(LocalDateTime to) {
        return QEvent.event.eventDate.loe(to);
    }

    private static BooleanExpression annotationContainsIgnoreCase(String text) {
        return QEvent.event.annotation.containsIgnoreCase(text);
    }

    private static BooleanExpression descriptionContainsIgnoreCase(String text) {
        return QEvent.event.description.containsIgnoreCase(text);
    }

    private static BooleanExpression paid(Boolean paid) {
        return QEvent.event.paid.eq(paid);
    }

    public static Predicate adminFilter(List<Long> users, List<Long> categories, List<EventState> states, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        final List<BooleanExpression> expressions = new ArrayList<BooleanExpression>();
        if (users != null && !users.isEmpty() && users.getFirst() != 0) {
            expressions.add(initiatorIdIn(users));
        }
        if (categories != null && !categories.isEmpty() && categories.getFirst() != 0) {
            expressions.add(categoriesIn(categories));
        }
        if (states != null && !states.isEmpty() && states.getFirst() != null) {
            expressions.add(statesIn(states));
        }
        if (rangeStart != null) {
            expressions.add(eventDateGoe(rangeStart));
        }
        if (rangeEnd != null) {
            expressions.add(eventDateLoe(rangeEnd));
        }
        if (!expressions.isEmpty()) {
            BooleanExpression expression = expressions.getFirst();
            if (expression == null) {
                return null;
            }
            for (int i = 1; i < expressions.size(); i++) {
                expression = expression.and(expressions.get(i));
            }
            return expression;
        }
        return null;
    }

    public static Predicate publicFilter(String text, List<Long> categories, LocalDateTime start,
                                         LocalDateTime end, Boolean paid) {
        final List<BooleanExpression> expressions = new ArrayList<BooleanExpression>();

        if (text != null && !text.isBlank()) {
            expressions.add(annotationContainsIgnoreCase(text));
            expressions.add(descriptionContainsIgnoreCase(text));
        }
        if (categories != null && !categories.isEmpty() && categories.getFirst() != 0) {
            expressions.add(categoriesIn(categories));
        }
        if (start != null && end != null) {
            expressions.add(eventDateGoe(start));
            expressions.add(eventDateLoe(end));
        } else {
            expressions.add(eventDateGoe(LocalDateTime.now()));
        }

        if (paid != null) {
            expressions.add(paid(paid));
        }

        expressions.add(isPublish());

        BooleanExpression expression = expressions.getFirst();
        if (expression == null) {
            return null;
        }
        for (int i = 1; i < expressions.size(); i++) {
            expression = expression.and(expressions.get(i));
        }
        return expression;
    }

}
