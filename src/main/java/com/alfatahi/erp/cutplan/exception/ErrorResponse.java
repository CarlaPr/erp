package com.alfatahi.erp.cutplan.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Classe de resposta padronizada para erros API
 */
@Data
@AllArgsConstructor
class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
}
