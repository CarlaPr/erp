package com.alfatahi.erp.entity;

/**
 * Define como a duração de uma recorrência é determinada.
 */
public enum RecurrenceEndType {
    /** Termina após uma quantidade fixa de repetições. */
    COUNT,
    /** Termina em uma data específica. */
    DATE,
    /** Recorrência infinita (até cancelamento manual). */
    INFINITE
}
