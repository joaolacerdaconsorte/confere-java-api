package br.com.confere.dto;

public record UsuarioResumoDto(
        Long id,
        String nome,
        String email,
        String perfil,
        MunicipioDto municipio,
        boolean fonteGrande,
        boolean audioAtivo
) {
}
