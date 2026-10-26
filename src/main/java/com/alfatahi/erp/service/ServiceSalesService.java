package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.ApprovedQuoteSaleDto;
import com.alfatahi.erp.dto.ServiceSalesDetailDto;
import com.alfatahi.erp.dto.ServiceSalesEntryDto;
import com.alfatahi.erp.dto.ServiceSalesGroupDto;
import com.alfatahi.erp.dto.ServiceSalesSummaryDto;
import com.alfatahi.erp.entity.QuoteItem;
import com.alfatahi.erp.repository.DashboardRepository;
import com.alfatahi.erp.util.ServiceTypeGrouper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class ServiceSalesService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final Comparator<Accumulator> BEST_SELLERS = Comparator
            .comparingLong((Accumulator a) -> a.items).reversed()
            .thenComparing(a -> a.amount, Comparator.reverseOrder())
            .thenComparing(a -> a.name, String.CASE_INSENSITIVE_ORDER);

    private final DashboardRepository dashboardRepository;

    public ServiceSalesService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public ServiceSalesSummaryDto summarize(FinancialPeriod period) {
        Map<String, Group> groups = new LinkedHashMap<>();
        long totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ApprovedQuoteSaleDto sale : dashboardRepository.approvedQuoteSales(period)) {
            List<QuoteItem> items = sale.items();
            if (items.isEmpty()) continue;
            BigDecimal[] values = allocate(sale.saleTotal(), items);
            for (int i = 0; i < items.size(); i++) {
                QuoteItem item = items.get(i);
                String title = ServiceTypeGrouper.displayName(item.getCategory());
                String groupName = ServiceTypeGrouper.group(title);
                Group group = groups.computeIfAbsent(ServiceTypeGrouper.normalize(groupName), key -> new Group(groupName));
                BigDecimal units = units(item);
                group.add(title, units, values[i], sale);
                totalItems++;
                totalAmount = totalAmount.add(values[i]);
            }
        }

        List<ServiceSalesGroupDto> result = new ArrayList<>();
        for (Group group : groups.values().stream().sorted(BEST_SELLERS).toList()) {
            List<ServiceSalesDetailDto> details = group.details.values().stream().sorted(BEST_SELLERS)
                    .map(Detail::toDto)
                    .toList();
            boolean showTitles = details.size() > 1 || !ServiceTypeGrouper.normalize(group.name)
                    .equals(ServiceTypeGrouper.normalize(details.get(0).name()));
            BigDecimal percentage = totalAmount.signum() > 0
                    ? group.amount.multiply(HUNDRED).divide(totalAmount, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            result.add(new ServiceSalesGroupDto(group.name, group.items, unitsLabel(group.units),
                    isFractional(group.units, group.items), group.amount,
                    percentage.min(HUNDRED), showTitles, details));
        }
        return new ServiceSalesSummaryDto(totalItems, totalAmount, result);
    }

    static BigDecimal[] allocate(BigDecimal saleTotal, List<QuoteItem> items) {
        int count = items.size();
        BigDecimal total = (saleTotal != null ? saleTotal : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal[] weights = new BigDecimal[count];
        BigDecimal weightSum = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            BigDecimal subtotal = items.get(i).getSubtotal();
            weights[i] = subtotal != null && subtotal.signum() > 0 ? subtotal : BigDecimal.ZERO;
            weightSum = weightSum.add(weights[i]);
        }
        if (weightSum.signum() == 0) {
            for (int i = 0; i < count; i++) weights[i] = BigDecimal.ONE;
            weightSum = BigDecimal.valueOf(count);
        }
        BigDecimal[] shares = new BigDecimal[count];
        BigDecimal remaining = total;
        for (int i = 0; i < count - 1; i++) {
            shares[i] = total.multiply(weights[i]).divide(weightSum, 2, RoundingMode.HALF_UP);
            remaining = remaining.subtract(shares[i]);
        }
        shares[count - 1] = remaining.max(BigDecimal.ZERO);
        return shares;
    }

    private static BigDecimal units(QuoteItem item) {
        BigDecimal itemQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
        if (item.getMeasurements().isEmpty()) return itemQuantity;
        return item.getMeasurements().stream()
                .map(m -> m.getQuantity() != null ? m.getQuantity() : itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String unitsLabel(BigDecimal units) {
        BigDecimal stripped = units.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString().replace('.', ',');
    }

    private static boolean isFractional(BigDecimal units, long items) {
        return units.compareTo(BigDecimal.valueOf(items)) != 0;
    }

    private static class Accumulator {
        final String name;
        long items;
        BigDecimal units = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        Accumulator(String name) { this.name = name; }

        void add(BigDecimal itemUnits, BigDecimal itemAmount) {
            items++;
            units = units.add(itemUnits);
            amount = amount.add(itemAmount);
        }
    }

    private static final class Entry extends Accumulator {
        final ApprovedQuoteSaleDto sale;

        Entry(ApprovedQuoteSaleDto sale) {
            super(sale.clientName());
            this.sale = sale;
        }

        ServiceSalesEntryDto toDto() {
            String workOrder = sale.workOrderNumber() != null && !sale.workOrderNumber().isBlank()
                    ? sale.workOrderNumber() : sale.quoteNumber();
            return new ServiceSalesEntryDto(name, workOrder, sale.quoteNumber(), sale.approvedDate(), items,
                    unitsLabel(units), isFractional(units, items), amount);
        }
    }

    private static final class Detail extends Accumulator {
        final Map<UUID, Entry> entries = new LinkedHashMap<>();

        Detail(String name) { super(name); }

        void add(BigDecimal itemUnits, BigDecimal itemAmount, ApprovedQuoteSaleDto sale) {
            entries.computeIfAbsent(sale.quoteId(), id -> new Entry(sale)).add(itemUnits, itemAmount);
            add(itemUnits, itemAmount);
        }

        ServiceSalesDetailDto toDto() {
            List<ServiceSalesEntryDto> sold = entries.values().stream()
                    .sorted(BEST_AMOUNT_FIRST)
                    .map(Entry::toDto)
                    .toList();
            return new ServiceSalesDetailDto(name, items, unitsLabel(units), isFractional(units, items), amount, sold);
        }
    }

    private static final Comparator<Accumulator> BEST_AMOUNT_FIRST = Comparator
            .comparing((Accumulator a) -> a.amount).reversed()
            .thenComparing(a -> a.name, String.CASE_INSENSITIVE_ORDER);

    private static final class Group extends Accumulator {
        final Map<String, Detail> details = new LinkedHashMap<>();

        Group(String name) { super(name); }

        void add(String title, BigDecimal itemUnits, BigDecimal itemAmount, ApprovedQuoteSaleDto sale) {
            details.computeIfAbsent(ServiceTypeGrouper.normalize(title), key -> new Detail(title))
                    .add(itemUnits, itemAmount, sale);
            add(itemUnits, itemAmount);
        }
    }
}
