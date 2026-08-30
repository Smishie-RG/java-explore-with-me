package ru.practicum.ewm.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
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
class CommentApiIntegrationTest {
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
    void shouldManageCommentByAuthor() throws Exception {
        long ownerId = createUser("comment-owner@mail.ru");
        long authorId = createUser("comment-author@mail.ru");
        long categoryId = createCategory("Comment events");
        long eventId = createEvent(ownerId, categoryId);
        publishEvent(eventId);

        long commentId = createComment(authorId, eventId, "First comment");

        mockMvc.perform(get("/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.author.id").value(authorId))
                .andExpect(jsonPath("$.event").value(eventId));

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", authorId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Updated comment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated comment"));

        mockMvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(commentId));

        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", authorId, commentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/comments/{commentId}", commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateNewComment() throws Exception {
        long ownerId = createUser("validation-owner@mail.ru");
        long authorId = createUser("validation-author@mail.ru");
        long categoryId = createCategory("Validation events");
        long eventId = createEvent(ownerId, categoryId);

        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", authorId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Comment\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isNotFound());

        publishEvent(eventId);
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", authorId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());

        String longText = objectMapper.writeValueAsString(Map.of("text", "x".repeat(2001)));
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", authorId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longText))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", 999999, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Comment\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", authorId, 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Comment\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/events/{eventId}/comments", eventId).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldProtectCommentAndAllowAdminDelete() throws Exception {
        long ownerId = createUser("access-owner@mail.ru");
        long authorId = createUser("access-author@mail.ru");
        long otherId = createUser("access-other@mail.ru");
        long categoryId = createCategory("Access events");
        long eventId = createEvent(ownerId, categoryId);
        publishEvent(eventId);
        long commentId = createComment(authorId, eventId, "Protected comment");

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", otherId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Other text\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", otherId, commentId))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnCommentsWithPagination() throws Exception {
        long ownerId = createUser("page-owner@mail.ru");
        long authorId = createUser("page-author@mail.ru");
        long categoryId = createCategory("Page events");
        long eventId = createEvent(ownerId, categoryId);
        publishEvent(eventId);
        long firstCommentId = createComment(authorId, eventId, "First page comment");
        long secondCommentId = createComment(authorId, eventId, "Second page comment");

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(secondCommentId));
        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstCommentId));
    }

    private long createComment(long userId, long eventId, String text) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("text", text));
        String response = mockMvc.perform(
                        post("/users/{userId}/events/{eventId}/comments", userId, eventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(text))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createUser(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Comment user", "email", email));
        String response = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createCategory(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name));
        String response = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createEvent(long userId, long categoryId) throws Exception {
        String date = LocalDateTime.now().plusDays(3).format(FORMATTER);
        String body = "{\"annotation\":\"Comment event annotation\","
                + "\"category\":" + categoryId + ","
                + "\"description\":\"Detailed description for comment event\","
                + "\"eventDate\":\"" + date + "\","
                + "\"location\":{\"lat\":55.75,\"lon\":37.61},"
                + "\"paid\":false,\"participantLimit\":0,"
                + "\"requestModeration\":false,\"title\":\"Comment event\"}";
        String response = mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode event = objectMapper.readTree(response);
        return event.get("id").asLong();
    }

    private void publishEvent(long eventId) throws Exception {
        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk());
    }
}
