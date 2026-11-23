package com.alfatahi.erp.util;

import com.alfatahi.erp.entity.WorkOrder;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WorkOrderStatus {
    public static final Set<String> COMPLETED = Set.of("completed", "delivered", "done", "concluida", "concluída");
    private static final Set<String> CANCELLED = Set.of("cancelled", "canceled");
    public static final Set<String> CLOSED = Stream.concat(COMPLETED.stream(), CANCELLED.stream())
            .collect(Collectors.toUnmodifiableSet());

    private WorkOrderStatus() {}

    private static String normalized(WorkOrder order) {
        return order.getStatus() == null ? "" : order.getStatus().trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isCompleted(WorkOrder order) {
        return COMPLETED.contains(normalized(order));
    }

    public static boolean isCancelled(WorkOrder order) {
        return CANCELLED.contains(normalized(order));
    }

    public static boolean isInProgress(WorkOrder order) {
        return !CLOSED.contains(normalized(order));
    }

    public static String fieldLabel(WorkOrder order) {
        if (isCompleted(order)) return "Concluída";
        return switch (normalized(order)) {
            case "", "in_progress", "em andamento" -> "Em andamento";
            case "pending", "open", "aberta" -> "Aberta";
            case "scheduled", "agendada" -> "Agendada";
            default -> order.getStatus().strip();
        };
    }
}
