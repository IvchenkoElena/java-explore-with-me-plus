package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.AdminUpdateCommentStatusDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.enums.AdminUpdateCommentStatusAction;
import ru.practicum.comment.enums.CommentStatus;
import ru.practicum.comment.enums.UpdateCommentAction;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.OperationForbiddenException;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional
    @Override
    public CommentDto createComment(long authorId, long eventId, NewCommentDto newCommentDto) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(String.format("User with ID %s not found", authorId)));
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, authorId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with ID %s not found", eventId)));
        Comment comment = commentMapper.toComment(newCommentDto, author, event);
        commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    @Transactional
    @Override
    public CommentDto updateComment(long authorId, long eventId, long commentId, UpdateCommentDto updateCommentDto) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(String.format("User with ID %s not found", authorId)));
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, authorId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with ID %s not found", eventId)));
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with ID %s not found", commentId)));
        if (updateCommentDto.getText() != null) {
            commentToUpdate.setText(updateCommentDto.getText());
        }
        if (updateCommentDto.getAction() != null) {
            if (updateCommentDto.getAction().equals(UpdateCommentAction.SEND_TO_REVIEW)) {
                commentToUpdate.setStatus(CommentStatus.PENDING);
            }
            if (updateCommentDto.getAction().equals(UpdateCommentAction.CANCEL_REVIEW)) {
                commentToUpdate.setStatus(CommentStatus.CANCELED);
        } else {
                commentToUpdate.setStatus(CommentStatus.PENDING);
            }

        }
        commentRepository.save(commentToUpdate);
        return commentMapper.toDto(commentToUpdate);
    }

    @Transactional
    @Override
    public void deleteComment(long authorId, long eventId, long commentId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(String.format("User with ID %s not found", authorId)));
        Event event = eventRepository.findByIdAndInitiator_Id(eventId, authorId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with ID %s not found", eventId)));
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with ID %s not found", commentId)));
        commentRepository.delete(commentToDelete);
    }

    @Transactional
    @Override
    public CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto updateCommentStatusDto) {
        Comment commentToUpdateStatus = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with ID %s not found", commentId)));
        if (!commentToUpdateStatus.getStatus().equals(CommentStatus.PENDING)) {
            throw new OperationForbiddenException("Can't reject not pending comment");
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.PUBLISH_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.PUBLISHED);
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.REJECT_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.REJECTED);
        }
        commentRepository.save(commentToUpdateStatus);
        return commentMapper.toDto(commentToUpdateStatus);
    }
}
