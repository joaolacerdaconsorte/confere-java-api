package br.com.confere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @NotBlank(message = "Token de push é obrigatório")
        @Size(max = 200)
        String expoPushToken
) {
}
