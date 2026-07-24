package com.alfatahi.erp.cutplan.exception;

import com.alfatahi.erp.cutplan.exception.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CutPlanExceptionHandler - Tratamento global de exceções
 *
 * Intercepta exceções lançadas nos controllers e retorna
 * respostas HTTP formatadas com mensagens claras
 *
 * HTTP Status Codes:
 * - 400 Bad Request: Dados inválidos
 * - 401 Unauthorized: Não autenticado
 * - 403 Forbidden: Sem permissão
 * - 404 Not Found: Recurso não encontrado
 * - 409 Conflict: Operação em conflito (status inválido, duplicata, etc)
 * - 500 Internal Server Error: Erro genérico do servidor
 */
@Slf4j
@RestControllerAdvice
public class CutPlanExceptionHandler extends ResponseEntityExceptionHandler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Tratamento para CutPlanNotFoundException
     */
    @ExceptionHandler(CutPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCutPlanNotFound(
            CutPlanNotFoundException ex,
            WebRequest request) {

        log.warn("Recurso não encontrado: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                ex.getMessage(),
                request,
                "RESOURCE_NOT_FOUND"
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Tratamento para CutPlanItemNotFoundException
     */
    @ExceptionHandler(CutPlanItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(
            CutPlanItemNotFoundException ex,
            WebRequest request) {

        log.warn("Item não encontrado: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Item não encontrado",
                ex.getMessage(),
                request,
                "ITEM_NOT_FOUND"
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Tratamento para InvalidCutPlanStatusException
     */
    @ExceptionHandler(InvalidCutPlanStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(
            InvalidCutPlanStatusException ex,
            WebRequest request) {

        log.warn("Status inválido para operação: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Status inválido para operação",
                ex.getMessage(),
                request,
                "INVALID_STATUS"
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Tratamento para DuplicateCutPlanException
     */
    @ExceptionHandler(DuplicateCutPlanException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateCutPlanException ex,
            WebRequest request) {

        log.warn("Recurso duplicado: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Recurso duplicado",
                ex.getMessage(),
                request,
                "DUPLICATE_RESOURCE"
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Tratamento para InvalidDimensionsException
     */
    @ExceptionHandler(InvalidDimensionsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDimensions(
            InvalidDimensionsException ex,
            WebRequest request) {

        log.warn("Dimensões inválidas: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Dimensões inválidas",
                ex.getMessage(),
                request,
                "INVALID_DIMENSIONS"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento para CostCalculationException
     */
    @ExceptionHandler(CostCalculationException.class)
    public ResponseEntity<ErrorResponse> handleCostCalculation(
            CostCalculationException ex,
            WebRequest request) {

        log.error("Erro ao calcular custo: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Erro ao calcular custo",
                ex.getMessage(),
                request,
                "CALCULATION_ERROR"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento para PriceNotFoundException
     */
    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePriceNotFound(
            PriceNotFoundException ex,
            WebRequest request) {

        log.warn("Preço não encontrado: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Preço não encontrado",
                ex.getMessage(),
                request,
                "PRICE_NOT_FOUND"
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Tratamento para LayoutOptimizationException
     */
    @ExceptionHandler(LayoutOptimizationException.class)
    public ResponseEntity<ErrorResponse> handleLayoutOptimization(
            LayoutOptimizationException ex,
            WebRequest request) {

        log.error("Erro ao otimizar layout: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Erro ao otimizar layout",
                ex.getMessage(),
                request,
                "OPTIMIZATION_ERROR"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento para LayoutOptimizationSheetLimitExceededException
     */
    @ExceptionHandler(LayoutOptimizationSheetLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleSheetLimitExceeded(
            LayoutOptimizationSheetLimitExceededException ex,
            WebRequest request) {

        log.warn("Limite de chapas excedido: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Limite de chapas excedido",
                ex.getMessage(),
                request,
                "SHEET_LIMIT_EXCEEDED"
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Tratamento para EntityNotFoundException (JPA)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request) {

        log.warn("Entidade não encontrada: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Entidade não encontrada",
                ex.getMessage(),
                request,
                "ENTITY_NOT_FOUND"
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Tratamento para IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            WebRequest request) {

        log.warn("Operação inválida: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Operação inválida para o estado atual",
                ex.getMessage(),
                request,
                "ILLEGAL_STATE"
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Tratamento para IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Argumento inválido: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Argumento inválido",
                ex.getMessage(),
                request,
                "INVALID_ARGUMENT"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento para AccessDeniedException (Spring Security)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {

        log.warn("Acesso negado: {}", ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "Você não tem permissão para acessar este recurso",
                request,
                "ACCESS_DENIED"
        );

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Tratamento para MethodArgumentNotValidException (validação @Valid)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        log.warn("Validação falhou: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validação falhou")
                .message("Um ou mais campos possuem valores inválidos")
                .path(request.getDescription(false).replace("uri=", ""))
                .traceId(UUID.randomUUID().toString())
                .validationErrors(errors)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento para exceções genéricas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Erro não tratado: ", ex);

        ErrorResponse error = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor",
                "Ocorreu um erro inesperado. Por favor, contate o suporte.",
                request,
                "INTERNAL_ERROR"
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Construir resposta de erro padronizada
     */
    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            WebRequest request,
            String code) {

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .traceId(UUID.randomUUID().toString())
                .code(code)
                .build();
    }
}