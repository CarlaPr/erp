package com.alfatahi.erp.cutplan.exception;

/**
 * CostCalculationException - Erro ao calcular custos
 */
class CostCalculationException extends CutPlanException {

    public CostCalculationException(String itemDescription, String reason) {
        super(String.format(
                "Erro ao calcular custo para item '%s': %s",
                itemDescription, reason
        ));
    }
}
