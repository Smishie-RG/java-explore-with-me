package ru.practicum.ewm.main.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable long userId,
                             @PathVariable long eventId,
                             @RequestBody @Valid CommentTextDto dto) {
        return commentService.create(userId, eventId, dto);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto update(@PathVariable long userId,
                             @PathVariable long commentId,
                             @RequestBody @Valid CommentTextDto dto) {
        return commentService.update(userId, commentId, dto);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long userId, @PathVariable long commentId) {
        commentService.deleteByAuthor(userId, commentId);
    }
}
