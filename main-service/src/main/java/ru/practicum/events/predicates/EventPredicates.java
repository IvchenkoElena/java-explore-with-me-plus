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

    private static BooleanExpression categoriesIn(List<Long> cIds) {
        return QEvent.event.category.id.in(cIds);
    }

    private static BooleanExpression eventDateGoe(LocalDateTime from) {
        return QEvent.event.eventDate.goe(from);
    }

    private static BooleanExpression eventDateLoe(LocalDateTime to) {
        return QEvent.event.eventDate.loe(to);
    }

    public static Predicate adminFilter(List<Long> users, List<Long> categories, List<EventState> states, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        BooleanExpression predicate;
        final List<BooleanExpression> expressions = new ArrayList<BooleanExpression>();
        if (users != null && !users.isEmpty()) {
            expressions.add(initiatorIdIn(users));
        }
        if (categories != null && !categories.isEmpty()) {
            expressions.add(categoriesIn(categories));
        }
        if (states != null && !states.isEmpty()) {
            expressions.add(statesIn(states));
        }
        if (rangeStart != null) {
            expressions.add(eventDateGoe(rangeStart));
        }
        if (rangeEnd != null) {
            expressions.add(eventDateLoe(rangeEnd));
        }
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
