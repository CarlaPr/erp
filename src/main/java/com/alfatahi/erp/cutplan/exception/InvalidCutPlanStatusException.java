package com.alfatahi.erp.cutplan.exception;

/**
 * InvalidCutPlanStatusException - Status inválido para operação
 */
class InvalidCutPlanStatusException extends CutPlanException {

    public InvalidCutPlanStatusException(String currentStatus, String operation) {
        super(String.format(
                "Operação '%s' não permitida para plano em status '%s'",
                operation, currentStatus
        ));
    }
}
