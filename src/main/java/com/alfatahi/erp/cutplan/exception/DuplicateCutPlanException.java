package com.alfatahi.erp.cutplan.exception;

/**
 * DuplicateCutPlanException - Plano duplicado para mesma OS
 */
class DuplicateCutPlanException extends CutPlanException {

    public DuplicateCutPlanException(String workOrderId) {
        super(String.format(
                "Já existe um plano de corte para a Ordem de Serviço: %s",
                workOrderId
        ));
    }
}
