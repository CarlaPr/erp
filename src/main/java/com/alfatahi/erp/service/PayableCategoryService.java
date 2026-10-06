package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.PayableCategorySummary;
import com.alfatahi.erp.dto.PayableCategoryTotals;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.ExpenseCategory;
import com.alfatahi.erp.entity.ExpenseSubcategory;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.ExpenseCategoryRepository;
import com.alfatahi.erp.repository.ExpenseSubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PayableCategoryService {
    public static final String UNCONFIGURED = "__UNCONFIGURED__";
    private final AccountsPayableRepository payableRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseSubcategoryRepository subcategoryRepository;

    public PayableCategoryService(AccountsPayableRepository payableRepository,
                                  ExpenseCategoryRepository categoryRepository,
                                  ExpenseSubcategoryRepository subcategoryRepository) {
        this.payableRepository = payableRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    public PayableCategorySummary summarize(FinancialPeriod period) {
        return summarizeTotals(payableRepository.summarizeCategoriesByPeriod(period.from(), period.endExclusive()));
    }

    public PayableCategorySummary summarize(List<AccountsPayable> payables) {
        return summarizeTotals(payables.stream()
                .filter(p -> !"cancelled".equals(p.getStatus()) && !"inactive".equals(p.getStatus()))
                .map(p -> new PayableCategoryTotals(p.getCategory(), p.getSubcategory(), 1, nvl(p.getTotalAmount()),
                        "paid".equals(p.getStatus()) || "partial".equals(p.getStatus())
                                ? nvl(p.getPaidAmount()) : BigDecimal.ZERO))
                .toList());
    }

    public List<AccountsPayable> filterByCategory(List<AccountsPayable> payables, String category) {
        if (category == null || category.isBlank()) return payables;
        if (UNCONFIGURED.equals(category)) {
            var catalog = catalog();
            return payables.stream().filter(p -> !catalog.containsKey(normalize(p.getCategory()))).toList();
        }
        return payables.stream().filter(p -> normalize(category).equals(normalize(p.getCategory()))).toList();
    }

    private PayableCategorySummary summarizeTotals(List<PayableCategoryTotals> totals) {
        var catalog = catalog();
        var subcategoryCatalog = subcategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(s -> s.getCategory() != null)
                .collect(Collectors.toMap(s -> normalize(s.getCategory().getCode()) + "::" + normalize(s.getCode()),
                        Function.identity(), (a, b) -> a));
        Map<GroupLabel, PayableCategoryTotals> grouped = new LinkedHashMap<>();
        Map<GroupLabel, PayableCategoryTotals> groupedSubcategories = new LinkedHashMap<>();
        for (var row : totals) {
            String code = normalize(row.category());
            if (!catalog.containsKey(code)) code = UNCONFIGURED;
            boolean unconfigured = UNCONFIGURED.equals(code);
            String name = unconfigured ? "Sem categoria configurada" : catalog.get(code).getName();
            accumulate(grouped, new GroupLabel(code, name, unconfigured), row);
            ExpenseSubcategory subcategory = subcategoryCatalog.get(code + "::" + normalize(row.subcategory()));
            String subcategoryCode = subcategory == null ? UNCONFIGURED : normalize(subcategory.getCode());
            String subcategoryName = subcategory == null ? "Sem subcategoria configurada" : subcategory.getName();
            accumulate(groupedSubcategories, new GroupLabel(code + "::" + subcategoryCode,
                    name + " / " + subcategoryName, unconfigured || subcategory == null), row);
        }
        long count = grouped.values().stream().mapToLong(PayableCategoryTotals::count).sum();
        BigDecimal totalAmount = grouped.values().stream().map(PayableCategoryTotals::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = grouped.values().stream().map(PayableCategoryTotals::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayableCategorySummary(summarizeGroups(grouped, count, totalAmount, paidAmount),
                summarizeGroups(groupedSubcategories, count, totalAmount, paidAmount), count, totalAmount, paidAmount);
    }

    private static void accumulate(Map<GroupLabel, PayableCategoryTotals> groups, GroupLabel label, PayableCategoryTotals row) {
        var previous = groups.getOrDefault(label,
                new PayableCategoryTotals(null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO));
        groups.put(label, new PayableCategoryTotals(null, null, previous.count() + row.count(),
                previous.totalAmount().add(nvl(row.totalAmount())), previous.paidAmount().add(nvl(row.paidAmount()))));
    }

    private List<PayableCategorySummary.Category> summarizeGroups(Map<GroupLabel, PayableCategoryTotals> groups,
                                                                 long count, BigDecimal totalAmount, BigDecimal paidAmount) {
        return groups.entrySet().stream().map(entry -> {
            var label = entry.getKey();
            var row = entry.getValue();
            return new PayableCategorySummary.Category(label.code(), label.name(), label.unconfigured(), row.count(),
                    row.totalAmount(), row.paidAmount(), percentage(BigDecimal.valueOf(row.count()), BigDecimal.valueOf(count)),
                    percentage(row.totalAmount(), totalAmount), percentage(row.paidAmount(), paidAmount));
        }).sorted(Comparator.comparing(PayableCategorySummary.Category::paidAmount).reversed()
                .thenComparing(Comparator.comparingLong(PayableCategorySummary.Category::count).reversed())
                .thenComparing(PayableCategorySummary.Category::name)).toList();
    }

    private record GroupLabel(String code, String name, boolean unconfigured) { }

    private Map<String, ExpenseCategory> catalog() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .collect(Collectors.toMap(c -> normalize(c.getCode()), Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static String normalize(String category) {
        return category == null ? "" : category.strip().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal total) {
        return total.signum() <= 0 ? BigDecimal.ZERO
                : value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
