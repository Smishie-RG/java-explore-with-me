package ru.practicum.ewm.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.mapper.EndpointHitMapper;
import ru.practicum.ewm.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        validateHit(hitDto);
        statsRepository.save(EndpointHitMapper.toEndpointHit(hitDto));
    }

    @Override
    public List<ViewStats> getStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Дата начала диапазона должна быть раньше даты окончания"
            );
        }

        boolean hasUris = uris != null && !uris.isEmpty();

        if (unique && hasUris) {
            return statsRepository.findUniqueStatsByUris(start, end, uris);
        }
        if (unique) {
            return statsRepository.findUniqueStats(start, end);
        }
        if (hasUris) {
            return statsRepository.findStatsByUris(start, end, uris);
        }
        return statsRepository.findStats(start, end);
    }

    private void validateHit(EndpointHitDto hitDto) {
        if (hitDto.getApp() == null || hitDto.getApp().isBlank()
                || hitDto.getUri() == null || hitDto.getUri().isBlank()
                || hitDto.getIp() == null || hitDto.getIp().isBlank()
                || hitDto.getTimestamp() == null) {
            throw new IllegalArgumentException(
                    "Данные о запросе должны быть заполнены"
            );
        }
    }
}
