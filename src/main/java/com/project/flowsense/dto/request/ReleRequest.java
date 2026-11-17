package com.project.flowsense.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo de POST /api/dispositivos/{id}/rele. Ex.: {"estado":"OFF"} */
public record ReleRequest(
        @NotNull(message = "informe o estado do relé")
        @Pattern(regexp = "ON|OFF", message = "o estado deve ser ON ou OFF")
        String estado) {
}
