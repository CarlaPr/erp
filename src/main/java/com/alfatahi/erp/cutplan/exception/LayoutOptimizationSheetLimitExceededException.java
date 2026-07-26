package com.alfatahi.erp.cutplan.exception;

/**
 * LayoutOptimizationSheetLimitExceededException - Limite de chapas excedido
 */
class LayoutOptimizationSheetLimitExceededException extends LayoutOptimizationException {

    public LayoutOptimizationSheetLimitExceededException(int maxSheets, int requiredSheets) {
        super(String.format(
                "Limite de chapas excedido: máximo %d, necessário %d",
                maxSheets, requiredSheets
        ));
    }
}
