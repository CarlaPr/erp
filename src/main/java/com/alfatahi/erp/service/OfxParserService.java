package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.BankTransaction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OfxParserService {

    public List<BankTransaction> parse(MultipartFile file) throws Exception {
        List<BankTransaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }

            String text = content.toString();
            // Procura todos os blocos de transação no OFX
            Matcher matcher = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>").matcher(text);

            while (matcher.find()) {
                String trn = matcher.group(1);
                BankTransaction tx = new BankTransaction();

                // 1. Extrair Data (DTPOSTED)
                Matcher dtMatcher = Pattern.compile("<DTPOSTED>(\\d{8})").matcher(trn);
                if (dtMatcher.find()) {
                    String dateStr = dtMatcher.group(1);
                    tx.setTransactionDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd")));
                } else {
                    tx.setTransactionDate(LocalDate.now());
                }

                // 2. Extrair Valor e Tipo (TRNAMT)
                Matcher amtMatcher = Pattern.compile("<TRNAMT>([\\-\\d\\.]+)").matcher(trn);
                if (amtMatcher.find()) {
                    BigDecimal amt = new BigDecimal(amtMatcher.group(1));
                    tx.setAmount(amt.abs());
                    tx.setType(amt.compareTo(BigDecimal.ZERO) >= 0 ? "IN" : "OUT");
                }

                // 3. Extrair Descrição (MEMO)
                Matcher memoMatcher = Pattern.compile("<MEMO>(.*?)(<|$)").matcher(trn);
                if (memoMatcher.find()) {
                    tx.setDescription(memoMatcher.group(1).trim());
                } else {
                    tx.setDescription("Transação Bancária Importada");
                }

                tx.setStatus("pending");
                transactions.add(tx);
            }
        }
        return transactions;
    }
}