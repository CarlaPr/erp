package com.alfatahi.erp.dto;

import java.math.BigDecimal;

public record BankAccountBalance(String name, BigDecimal openingBalance, BigDecimal currentBalance) {
}
