package com.alfatahi.erp.cutplan.exception;

/**
 * Exceções customizadas para o módulo Plano de Corte
 *
 * Permitem tratamento específico de erros e mensagens claras para o cliente
 */

/**
 * CutPlanException - Exceção base para o módulo
 */
public abstract class CutPlanException extends RuntimeException {

    public CutPlanException(String message) {
        super(message);
    }

    public CutPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}

