package com.alfatahi.erp.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ServiceTypeGrouper {
    public static final String UNTITLED = "Item sem título";

    private record Rule(Predicate<String> matches, String label) { }

    private static final List<Rule> RULES = List.of(
            new Rule(n -> n.startsWith("box"), "Box"),
            new Rule(n -> n.contains("guarda") && n.contains("corpo"), "Guarda-Corpo"),
            new Rule(n -> n.contains("sacada") && n.contains("manuten"), "Manutenção de Sacada"),
            new Rule(n -> n.contains("sacada") && n.contains("fechamento"), "Fechamento de Sacada"),
            new Rule(n -> n.contains("sacada"), "Sacada"),
            new Rule(n -> n.startsWith("manuten"), "Manutenção"),
            new Rule(n -> n.startsWith("espelho"), "Espelho"),
            new Rule(n -> n.startsWith("cobertura"), "Cobertura"),
            new Rule(n -> n.startsWith("janela"), "Janela"),
            new Rule(n -> n.startsWith("porta"), "Porta"));

    private static final Pattern DASH_SEPARATOR = Pattern.compile("\\s+[-–—]\\s+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private ServiceTypeGrouper() { }

    public static String displayName(String category) {
        if (category == null) return UNTITLED;
        String cleaned = WHITESPACE.matcher(category.trim()).replaceAll(" ");
        return cleaned.isEmpty() ? UNTITLED : cleaned;
    }

    public static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String plain = DIACRITICS.matcher(decomposed).replaceAll("");
        return WHITESPACE.matcher(plain.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
    }

    public static String group(String category) {
        String title = displayName(category);
        String normalized = normalize(title);
        for (Rule rule : RULES) {
            if (rule.matches().test(normalized)) return rule.label();
        }
        String[] parts = DASH_SEPARATOR.split(title, 2);
        return parts[0].isBlank() ? title : parts[0].trim();
    }
}
