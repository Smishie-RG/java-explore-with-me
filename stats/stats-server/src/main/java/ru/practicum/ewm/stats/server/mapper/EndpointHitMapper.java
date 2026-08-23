package ru.practicum.ewm.stats.server.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.server.model.EndpointHit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EndpointHitMapper {
    public static EndpointHit toEndpointHit(EndpointHitDto hitDto) {
        return new EndpointHit(
                null,
                hitDto.getApp(),
                hitDto.getUri(),
                hitDto.getIp(),
                hitDto.getTimestamp()
        );
    }
}
