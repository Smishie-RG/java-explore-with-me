package ru.practicum.ewm.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MainApiIntegrationTest {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        when(statsClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());
    }

    @Test
    void shouldManageUsersAndCategories() throws Exception {
        long userId = createUser("user@mail.ru");

        mockMvc.perform(get("/admin/users").param("ids", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("user@mail.ru"));
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        long categoryId = createCategory("Concerts");
        mockMvc.perform(patch("/admin/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Music\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Music"));

        mockMvc.perform(get("/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId));
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/admin/categories/{id}", categoryId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/admin/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldValidateUsersCategoriesAndPagination() throws Exception {
        createUser("unique@mail.ru");
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"email\":\"unique@mail.ru\"}"))
                .andExpect(status().isConflict());

        createCategory("Theatre");
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Theatre\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/categories").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateUpdatePublishAndFindEvent() throws Exception {
        long userId = createUser("owner@mail.ru");
        long categoryId = createCategory("Exhibitions");
        JsonNode event = createEvent(userId, categoryId, 5, true);
        long eventId = event.get("id").asLong();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated event\",\"stateAction\":\"CANCEL_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELED"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"SEND_TO_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"));

        publishEvent(eventId);

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated event"))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        mockMvc.perform(get("/events")
                        .param("text", "detailed")
                        .param("categories", String.valueOf(categoryId))
                        .param("sort", "VIEWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        verify(statsClient, atLeastOnce()).saveHit(any());
    }

    @Test
    void shouldRejectInvalidEventOperations() throws Exception {
        long userId = createUser("dates@mail.ru");
        long categoryId = createCategory("Lectures");
        String invalidDate = LocalDateTime.now().plusMinutes(30).format(FORMATTER);

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(categoryId, 1, true, invalidDate)))
                .andExpect(status().isBadRequest());

        long eventId = createEvent(userId, categoryId, 1, true).get("id").asLong();
        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isNotFound());
        publishEvent(eventId);
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Forbidden update\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldUpdateAllEventFieldsAndSearchByAdmin() throws Exception {
        long userId = createUser("admin-search@mail.ru");
        long firstCategoryId = createCategory("Cinema");
        long secondCategoryId = createCategory("Education");
        long eventId = createEvent(userId, firstCategoryId, 4, true).get("id").asLong();
        String newDate = LocalDateTime.now().plusDays(5).format(FORMATTER);
        String updateBody = "{\"annotation\":\"Completely updated annotation\","
                + "\"category\":" + secondCategoryId + ","
                + "\"description\":\"Completely updated event description\","
                + "\"eventDate\":\"" + newDate + "\","
                + "\"location\":{\"lat\":10.5,\"lon\":20.5},"
                + "\"paid\":true,\"participantLimit\":2,"
                + "\"requestModeration\":false,\"title\":\"Admin update\"}";

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(secondCategoryId))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.participantLimit").value(2));

        mockMvc.perform(get("/users/{userId}/events", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin update"));

        mockMvc.perform(get("/admin/events")
                        .param("users", String.valueOf(userId))
                        .param("states", "PENDING")
                        .param("categories", String.valueOf(secondCategoryId))
                        .param("rangeStart", LocalDateTime.now().format(FORMATTER))
                        .param("rangeEnd", LocalDateTime.now().plusDays(10).format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldConfirmAndRejectRequestsWhenLimitReached() throws Exception {
        long ownerId = createUser("request-owner@mail.ru");
        long firstUserId = createUser("first@mail.ru");
        long secondUserId = createUser("second@mail.ru");
        long categoryId = createCategory("Trips");
        long eventId = createEvent(ownerId, categoryId, 1, true).get("id").asLong();
        publishEvent(eventId);

        long firstRequestId = createRequest(firstUserId, eventId, "PENDING");
        long secondRequestId = createRequest(secondUserId, eventId, "PENDING");
        String body = "{\"requestIds\":[" + firstRequestId
                + "],\"status\":\"CONFIRMED\"}";
        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", ownerId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.rejectedRequests[0].id").value(secondRequestId));

        mockMvc.perform(get("/users/{userId}/requests", secondUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", ownerId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateAutomaticAndCanceledRequests() throws Exception {
        long ownerId = createUser("auto-owner@mail.ru");
        long participantId = createUser("participant@mail.ru");
        long categoryId = createCategory("Meetings");
        long unlimitedEventId = createEvent(ownerId, categoryId, 0, true).get("id").asLong();
        publishEvent(unlimitedEventId);
        createRequest(participantId, unlimitedEventId, "CONFIRMED");

        long limitedEventId = createEvent(ownerId, categoryId, 2, true).get("id").asLong();
        publishEvent(limitedEventId);
        long requestId = createRequest(participantId, limitedEventId, "PENDING");
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel",
                        participantId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void shouldHandleRequestConflictsAndRejection() throws Exception {
        long ownerId = createUser("conflict-owner@mail.ru");
        long firstUserId = createUser("conflict-first@mail.ru");
        long secondUserId = createUser("conflict-second@mail.ru");
        long categoryId = createCategory("Sports");
        long eventId = createEvent(ownerId, categoryId, 2, true).get("id").asLong();

        mockMvc.perform(post("/users/{userId}/requests", firstUserId)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isConflict());
        publishEvent(eventId);
        mockMvc.perform(post("/users/{userId}/requests", ownerId)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isConflict());

        long requestId = createRequest(firstUserId, eventId, "PENDING");
        mockMvc.perform(post("/users/{userId}/requests", firstUserId)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", ownerId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String rejectBody = "{\"requestIds\":[" + requestId
                + "],\"status\":\"REJECTED\"}";
        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", ownerId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectedRequests[0].status").value("REJECTED"));

        long automaticEventId = createEvent(ownerId, categoryId, 1, false).get("id").asLong();
        publishEvent(automaticEventId);
        long automaticRequestId = createRequest(firstUserId, automaticEventId, "CONFIRMED");
        mockMvc.perform(post("/users/{userId}/requests", secondUserId)
                        .param("eventId", String.valueOf(automaticEventId)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel",
                        firstUserId, automaticRequestId))
                .andExpect(status().isOk());
        createRequest(secondUserId, automaticEventId, "CONFIRMED");
    }

    @Test
    void shouldManageCompilations() throws Exception {
        long userId = createUser("compilation@mail.ru");
        long categoryId = createCategory("Festivals");
        long eventId = createEvent(userId, categoryId, 10, false).get("id").asLong();
        String createBody = "{\"title\":\"Summer events\",\"pinned\":true,\"events\":["
                + eventId + "]}";
        String response = mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long compilationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/admin/compilations/{id}", compilationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Empty selection\",\"pinned\":false,\"events\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(0)))
                .andExpect(jsonPath("$.pinned").value(false));

        mockMvc.perform(get("/compilations/{id}", compilationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Empty selection"));
        mockMvc.perform(get("/compilations").param("pinned", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(delete("/admin/compilations/{id}", compilationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/compilations/{id}", compilationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForMissingResources() throws Exception {
        long userId = createUser("missing@mail.ru");
        long categoryId = createCategory("Temporary");

        mockMvc.perform(get("/categories/{id}", categoryId + 1000))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/admin/categories/{id}", categoryId + 1000))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/admin/users/{id}", userId + 1000))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/compilations/{id}", 999999))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/admin/compilations/{id}", 999999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, 999999))
                .andExpect(status().isNotFound());
    }

    private long createUser(String email) throws Exception {
        String body = "{\"name\":\"Test user\",\"email\":\"" + email + "\"}";
        String response = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createCategory(String name) throws Exception {
        String body = "{\"name\":\"" + name + "\"}";
        String response = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private JsonNode createEvent(long userId, long categoryId, int limit,
                                 boolean moderation) throws Exception {
        String date = LocalDateTime.now().plusDays(3).format(FORMATTER);
        String response = mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(categoryId, limit, moderation, date)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String eventJson(long categoryId, int limit, boolean moderation, String date) {
        return "{\"annotation\":\"Detailed event annotation\","
                + "\"category\":" + categoryId + ","
                + "\"description\":\"Detailed description of the test event\","
                + "\"eventDate\":\"" + date + "\","
                + "\"location\":{\"lat\":55.75,\"lon\":37.61},"
                + "\"paid\":false,"
                + "\"participantLimit\":" + limit + ","
                + "\"requestModeration\":" + moderation + ","
                + "\"title\":\"Test event\"}";
    }

    private void publishEvent(long eventId) throws Exception {
        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    private long createRequest(long userId, long eventId, String statusValue) throws Exception {
        String response = mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(statusValue))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
