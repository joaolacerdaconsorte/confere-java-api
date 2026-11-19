package br.com.confere.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConfereException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ConfereException(HttpStatus status, String codigo, String message) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }
}
