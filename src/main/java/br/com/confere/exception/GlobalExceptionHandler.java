package br.com.confere.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE_URI = "https://confere.app/erros/";

    @ExceptionHandler(ConfereException.class)
    public ProblemDetail handleConfereException(ConfereException ex, HttpServletRequest request) {
        return problema(ex.getStatus(), titleForStatus(ex.getStatus()), ex.getMessage(), ex.getCodigo(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorMap)
                .toList();

        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos.", "VALIDACAO", request);
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

        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais parâmetros estão inválidos.", "VALIDACAO", request);
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Corpo da requisição inválido ou malformado.", "VALIDACAO", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "O parâmetro '" + ex.getName() + "' tem um valor inválido.", "VALIDACAO", request);
        pd.setProperty("erros", List.of(Map.of(
                "campo", ex.getName(),
                "mensagem", "Valor inválido: " + ex.getValue()
        )));
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório.", "VALIDACAO", request);
        pd.setProperty("erros", List.of(Map.of(
                "campo", ex.getParameterName(),
                "mensagem", "Parâmetro obrigatório"
        )));
        return pd;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "A parte '" + ex.getRequestPartName() + "' do formulário é obrigatória.", "VALIDACAO", request);
        pd.setProperty("erros", List.of(Map.of(
                "campo", ex.getRequestPartName(),
                "mensagem", "Parte obrigatória do multipart"
        )));
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "Arquivo muito grande",
                "Arquivo excede o limite máximo de 5 MB.", "VALIDACAO", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado",
                "Use application/json ou multipart/form-data conforme a documentação.", "VALIDACAO", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "O método " + ex.getMethod() + " não é aceito nesta rota.", "METODO_NAO_PERMITIDO", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                "A rota informada não existe.", "NAO_ENCONTRADO", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito de dados",
                "A operação viola uma regra do banco, por exemplo um registro em uso por outra tabela ou um valor duplicado.",
                "CONFLITO", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado",
                "E-mail ou senha incorretos.", "NAO_AUTENTICADO", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.", "SEM_PERMISSAO", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().value() < 500) {
            HttpStatusCode statusCode = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.valueOf(statusCode.value());
            return problema(status, titleForStatus(status), errorResponse.getBody().getDetail() != null
                    ? errorResponse.getBody().getDetail() : status.getReasonPhrase(), "REQUISICAO_INVALIDA", request);
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente em instantes.", "ERRO_INTERNO", request);
    }

    private ProblemDetail problema(HttpStatus status, String title, String detail, String codigo, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE_TYPE_URI + codigo.toLowerCase()));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("codigo", codigo);
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
