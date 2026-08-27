package ru.practicum.ewm.main.request;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipationRequestMapper {
    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return new ParticipationRequestDto(request.getCreated(), request.getEvent().getId(),
                request.getId(), request.getRequester().getId(), request.getStatus());
    }
}
