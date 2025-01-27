package ru.practicum.comment.service;

import ru.practicum.comment.dto.AdminUpdateCommentStatusDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;

public interface CommentService {
    CommentDto createComment(long userId, long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(long userId, long eventId, long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(long userId, long eventId, long commentId);

    CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto dto);
}
