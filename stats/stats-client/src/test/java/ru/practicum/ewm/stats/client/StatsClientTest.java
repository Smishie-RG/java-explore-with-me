package ru.practicum.ewm.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsClientTest {
    private static final String SERVER_URL = "http://localhost:9090";

    private MockRestServiceServer server;
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        statsClient = new StatsClient(SERVER_URL, restTemplate);
    }

    @Test
    void shouldSendHit() {
        EndpointHitDto hitDto = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.168.0.1",
                LocalDateTime.of(2026, 8, 22, 12, 30)
        );

        server.expect(requestTo(SERVER_URL + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\"app\":\"ewm-main-service\"," +
                                "\"uri\":\"/events/1\"," +
                                "\"ip\":\"192.168.0.1\"," +
                                "\"timestamp\":\"2026-08-22 12:30:00\"}"
                ))
                .andRespond(withNoContent());

        statsClient.saveHit(hitDto);

        server.verify();
    }

    @Test
    void shouldGetStatsWithEncodedParameters() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 22, 10, 0);

        server.expect(request -> {
                    String query = URLDecoder.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(request.getURI().getPath()).isEqualTo("/stats");
                    assertThat(query).contains("start=2026-08-20 10:00:00");
                    assertThat(query).contains("end=2026-08-22 10:00:00");
                    assertThat(query).contains("uris=/events");
                    assertThat(query).contains("uris=/events/1");
                    assertThat(query).contains("unique=true");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"app\":\"ewm-main-service\"," +
                                "\"uri\":\"/events\",\"hits\":3}]",
                        MediaType.APPLICATION_JSON
                ));

        List<ViewStats> result = statsClient.getStats(
                start,
                end,
                List.of("/events", "/events/1"),
                true
        );

        assertThat(result).containsExactly(
                new ViewStats("ewm-main-service", "/events", 3L)
        );
        server.verify();
    }

    @Test
    void shouldReturnEmptyListForEmptyResponse() {
        server.expect(request ->
                        assertThat(request.getURI().getPath())
                                .isEqualTo("/stats"))
                .andRespond(withSuccess());

        List<ViewStats> result = statsClient.getStats(
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 22, 10, 0),
                null,
                false
        );

        assertThat(result).isEmpty();
        server.verify();
    }
}
