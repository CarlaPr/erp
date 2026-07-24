package com.alfatahi.erp.cutplan.exception;

/**
 * InvalidDimensionsException - Dimensões inválidas após aplicar regras
 */
class InvalidDimensionsException extends CutPlanException {

    public InvalidDimensionsException(double width, double height) {
        super(String.format(
                "Dimensões inválidas após aplicar regras. Largura: %.2fmm, Altura: %.2fmm",
                width, height
        ));
    }
}
