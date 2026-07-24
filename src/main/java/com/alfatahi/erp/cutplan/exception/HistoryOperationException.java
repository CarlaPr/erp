package com.alfatahi.erp.cutplan.exception;

/**
 * HistoryOperationException - Erro ao gerenciar histórico
 */
class HistoryOperationException extends CutPlanException {

    public HistoryOperationException(String operation, String reason) {
        super(String.format(
                "Erro na operação de histórico '%s': %s",
                operation, reason
        ));
    }
}
