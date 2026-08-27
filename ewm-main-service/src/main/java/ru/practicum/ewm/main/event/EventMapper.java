package ru.practicum.ewm.main.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.category.Category;
import ru.practicum.ewm.main.category.CategoryMapper;
import ru.practicum.ewm.main.user.User;
import ru.practicum.ewm.main.user.UserMapper;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {
    public static Event toEvent(NewEventDto dto, Category category, User initiator) {
        Boolean paid = dto.getPaid() == null ? false : dto.getPaid();
        Integer participantLimit = dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit();
        Boolean requestModeration = dto.getRequestModeration() == null || dto.getRequestModeration();
        return new Event(null, dto.getAnnotation(), category, 0L, LocalDateTime.now(),
                dto.getDescription(), dto.getEventDate(), initiator, dto.getLocation(), paid,
                participantLimit, null, requestModeration, EventState.PENDING, dto.getTitle());
    }

    public static EventFullDto toEventFullDto(Event event, long views) {
        return new EventFullDto(event.getAnnotation(), CategoryMapper.toCategoryDto(event.getCategory()),
                event.getConfirmedRequests(), event.getCreatedOn(), event.getDescription(),
                event.getEventDate(), event.getId(), UserMapper.toUserShortDto(event.getInitiator()),
                event.getLocation(), event.getPaid(), event.getParticipantLimit(), event.getPublishedOn(),
                event.getRequestModeration(), event.getState(), event.getTitle(), views);
    }

    public static EventShortDto toEventShortDto(Event event, long views) {
        return new EventShortDto(event.getAnnotation(), CategoryMapper.toCategoryDto(event.getCategory()),
                event.getConfirmedRequests(), event.getEventDate(), event.getId(),
                UserMapper.toUserShortDto(event.getInitiator()), event.getPaid(), event.getTitle(), views);
    }
}
