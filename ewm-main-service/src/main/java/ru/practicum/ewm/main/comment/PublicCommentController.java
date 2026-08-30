package ru.practicum.ewm.main.comment;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/comments/{commentId}")
    public CommentDto getById(@PathVariable long commentId) {
        return commentService.getById(commentId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getEventComments(
            @PathVariable long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return commentService.getEventComments(eventId, from, size);
    }
}
