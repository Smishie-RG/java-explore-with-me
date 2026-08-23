package ru.practicum.ewm.stats.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.exception.ErrorHandler;
import ru.practicum.ewm.stats.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import(ErrorHandler.class)
class StatsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void shouldSaveHit() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"app\":\"ewm-main-service\"," +
                                "\"uri\":\"/events/1\"," +
                                "\"ip\":\"192.168.0.1\"," +
                                "\"timestamp\":\"2026-08-22 12:30:00\"}"))
                .andExpect(status().isCreated());

        verify(statsService).saveHit(any());
    }

    @Test
    void shouldReturnStats() throws Exception {
        when(statsService.getStats(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyList(),
                anyBoolean()
        )).thenReturn(List.of(
                new ViewStats("ewm-main-service", "/events", 3L)
        ));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-08-20 10:00:00")
                        .param("end", "2026-08-22 10:00:00")
                        .param("uris", "/events")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app")
                        .value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events"))
                .andExpect(jsonPath("$[0].hits").value(3));
    }

    @Test
    void shouldReturnBadRequestForInvalidRange() throws Exception {
        when(statsService.getStats(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(),
                anyBoolean()
        )).thenThrow(new IllegalArgumentException("Некорректный диапазон"));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-08-22 10:00:00")
                        .param("end", "2026-08-20 10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Некорректный диапазон"));
    }
}
