package ru.practicum.ewm.main.request;

import java.util.List;

public interface ParticipationRequestService {
    ParticipationRequestDto create(long userId, long eventId);

    List<ParticipationRequestDto> getUserRequests(long userId);

    ParticipationRequestDto cancel(long userId, long requestId);

    List<ParticipationRequestDto> getEventRequests(long userId, long eventId);

    EventRequestStatusUpdateResult updateStatuses(long userId, long eventId,
                                                  EventRequestStatusUpdateRequest updateRequest);
}
