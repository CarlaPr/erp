package com.alfatahi.erp.cutplan.exception;

/**
 * PriceNotFoundException - Preço não encontrado
 */
class PriceNotFoundException extends CutPlanException {

    public PriceNotFoundException(String category, String itemType) {
        super(String.format(
                "Nenhum preço encontrado para: %s - %s",
                category, itemType
        ));
    }
}
