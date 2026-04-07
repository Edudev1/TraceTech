package com.tracetech.eah.tracetecheah.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "Introduce tu contraseña actual")
    private String currentPassword;

    @NotBlank(message = "Introduce la nueva contraseña")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirma la nueva contraseña")
    private String confirmPassword;
}