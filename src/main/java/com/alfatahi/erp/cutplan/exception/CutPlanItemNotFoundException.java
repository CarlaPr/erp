package com.alfatahi.erp.cutplan.exception;

/**
 * CutPlanItemNotFoundException - Item não encontrado
 */
class CutPlanItemNotFoundException extends CutPlanException {

    public CutPlanItemNotFoundException(String itemId) {
        super(String.format("Item não encontrado: %s", itemId));
    }
}
