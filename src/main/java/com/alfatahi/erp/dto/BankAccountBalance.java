package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountBalance(UUID id, String name, BigDecimal openingBalance, BigDecimal currentBalance) {
}
