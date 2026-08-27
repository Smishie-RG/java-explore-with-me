package ru.practicum.ewm.main.event;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto create(long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(long userId, int from, int size);

    EventFullDto getUserEvent(long userId, long eventId);

    EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest request);

    List<EventFullDto> searchAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                   LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                     LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                     boolean onlyAvailable, EventSort sort, int from, int size,
                                     HttpServletRequest request);

    EventFullDto getPublicEvent(long eventId, HttpServletRequest request);
}
