package com.alfatahi.erp.cutplan.exception;

/**
 * CutPlanNotFoundException - Plano não encontrado
 */
class CutPlanNotFoundException extends CutPlanException {

    public CutPlanNotFoundException(String cutPlanId) {
        super(String.format("Plano de corte não encontrado: %s", cutPlanId));
    }
}
