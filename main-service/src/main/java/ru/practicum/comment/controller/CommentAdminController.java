package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.AdminUpdateCommentStatusDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "admin/comments")
public class CommentAdminController {
    private final CommentService commentService;

    @PatchMapping("/{commentId}")
    public CommentDto adminUpdateCommentStatus(@PathVariable("commentId") Long commentId,
                                               @Valid @RequestBody AdminUpdateCommentStatusDto dto) {
        return commentService.adminUpdateCommentStatus(commentId, dto);
    }

}
