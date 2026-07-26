package com.alfatahi.erp.cutplan.exception;

/**
 * LayoutOptimizationException - Erro ao otimizar layout
 */
class LayoutOptimizationException extends CutPlanException {

    public LayoutOptimizationException(String reason) {
        super(String.format("Erro ao otimizar layout: %s", reason));
    }
}
