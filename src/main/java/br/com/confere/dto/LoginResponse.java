package br.com.confere.dto;

import java.time.OffsetDateTime;

public record LoginResponse(
        String token,
        String tipo,
        OffsetDateTime expiraEm,
        UsuarioResumoDto usuario
) {
}
