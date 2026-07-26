package com.alfatahi.erp.cutplan.exception;

/**
 * TechnicalRuleApplicationException - Erro ao aplicar regra técnica
 */
class TechnicalRuleApplicationException extends CutPlanException {

    public TechnicalRuleApplicationException(String ruleParameter, String reason) {
        super(String.format(
                "Erro ao aplicar regra técnica '%s': %s",
                ruleParameter, reason
        ));
    }
}
