package com.alfatahi.erp.entity;

/**
 * Situação de uma série de recorrência de Contas a Pagar.
 */
public enum RecurrenceStatus {
    /** Ainda gera novos lançamentos (quando aplicável). */
    ACTIVE,
    /** Cancelada pelo usuário — não gera mais lançamentos futuros. */
    CANCELLED,
    /** Atingiu o fim natural da recorrência (quantidade ou data final). */
    COMPLETED
}
