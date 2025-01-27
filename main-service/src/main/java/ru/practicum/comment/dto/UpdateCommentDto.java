package ru.practicum.comment.dto;

import lombok.Data;
import ru.practicum.comment.enums.UpdateCommentAction;

@Data
public class UpdateCommentDto {
    private String text;

    private UpdateCommentAction action;
}
