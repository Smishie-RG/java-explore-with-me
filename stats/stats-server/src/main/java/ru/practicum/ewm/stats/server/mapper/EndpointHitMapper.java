package ru.practicum.ewm.stats.server.mapper;

import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.server.model.EndpointHit;

public final class EndpointHitMapper {
    private EndpointHitMapper() {
    }

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
