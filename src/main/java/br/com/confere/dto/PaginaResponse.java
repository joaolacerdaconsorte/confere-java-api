package br.com.confere.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas
) {
    public static <T> PaginaResponse<T> of(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
