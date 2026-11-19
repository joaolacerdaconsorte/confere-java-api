package br.com.confere.dto;

public record TipoGolpeDto(
        Long id,
        String codigo,
        String nome,
        String descricao,
        Integer gravidade,
        String orgaoImitado
) {
}
