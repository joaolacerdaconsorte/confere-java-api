package br.com.confere.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE_URI = "https://confere.app/erros/";

    @ExceptionHandler(ConfereException.class)
    public ProblemDetail handleConfereException(ConfereException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setType(URI.create(BASE_TYPE_URI + ex.getCodigo().toLowerCase()));
        pd.setTitle(titleForStatus(ex.getStatus()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", ex.getCodigo());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorMap)
                .toList();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        pd.setType(URI.create(BASE_TYPE_URI + "validacao"));
        pd.setTitle("Dados inválidos");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "VALIDACAO");
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> erros = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "campo", v.getPropertyPath().toString(),
                        "mensagem", v.getMessage()
                ))
                .toList();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais parâmetros estão inválidos.");
        pd.setType(URI.create(BASE_TYPE_URI + "validacao"));
        pd.setTitle("Dados inválidos");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "VALIDACAO");
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
        pd.setType(URI.create(BASE_TYPE_URI + "validacao"));
        pd.setTitle("Dados inválidos");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "VALIDACAO");
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Arquivo excede o limite máximo de 5 MB.");
        pd.setType(URI.create(BASE_TYPE_URI + "validacao"));
        pd.setTitle("Arquivo muito grande");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "VALIDACAO");
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        pd.setType(URI.create(BASE_TYPE_URI + "nao_autenticado"));
        pd.setTitle("Não autenticado");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "NAO_AUTENTICADO");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
        pd.setType(URI.create(BASE_TYPE_URI + "sem_permissao"));
        pd.setTitle("Acesso negado");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", "SEM_PERMISSAO");
        return pd;
    }

    private Map<String, String> toFieldErrorMap(FieldError fe) {
        return Map.of(
                "campo", fe.getField(),
                "mensagem", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido"
        );
    }

    private String titleForStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Dados inválidos";
            case UNAUTHORIZED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            case CONFLICT -> "Conflito de dados";
            case SERVICE_UNAVAILABLE -> "Serviço indisponível";
            default -> status.getReasonPhrase();
        };
    }
}
