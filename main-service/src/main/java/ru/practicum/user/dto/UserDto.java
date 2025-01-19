package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long id;
    @NotBlank
    @NotNull
    @Size(min = 2, max = 250)
    private String name;
    @Email
    @NotBlank
    @NotNull
    @Size(min = 6, max = 254)
    private String email;
}
