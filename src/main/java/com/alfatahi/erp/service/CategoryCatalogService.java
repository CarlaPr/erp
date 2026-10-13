package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.ExpenseCategory;
import com.alfatahi.erp.entity.ExpenseSubcategory;
import com.alfatahi.erp.repository.ExpenseCategoryRepository;
import com.alfatahi.erp.repository.ExpenseSubcategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class CategoryCatalogService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseSubcategoryRepository subcategoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryCatalogService(ExpenseCategoryRepository categoryRepository,
                                   ExpenseSubcategoryRepository subcategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    public List<ExpenseCategory> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }


    public List<ExpenseCategory> listFilterCategories(Collection<String> usedCodes) {
        Set<String> used = new HashSet<>();
        if (usedCodes != null) {
            for (String code : usedCodes) {
                if (code != null) used.add(code.strip().toUpperCase(Locale.ROOT));
            }
        }
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()) || used.contains(c.getCode().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    public String resolveExpenseType(String categoryCode, String subcategoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) return null;
        if (subcategoryCode != null && !subcategoryCode.isBlank()) {
            List<ExpenseSubcategory> found =
                    subcategoryRepository.findByCategoryCodeAndCode(categoryCode.strip(), subcategoryCode.strip());
            if (!found.isEmpty()) {
                ExpenseSubcategory sub = found.get(0);
                String group = sub.getDreGroup();
                if (group != null && group.startsWith("FORA_")) return "FORA_DRE";
                if (sub.getCostBehavior() != null && !sub.getCostBehavior().isBlank()) return sub.getCostBehavior();
            }
        }
        return categoryRepository.findByCode(categoryCode.strip().toUpperCase(Locale.ROOT))
                .map(category -> {
                    String group = category.getDreGroup();
                    if (group == null || group.isBlank()) return null;
                    if (group.startsWith("FORA_")) return "FORA_DRE";
                    if (group.startsWith("CMV_") || group.startsWith("DED_")) return "VARIAVEL";
                    return "FIXA";
                })
                .orElse(null);
    }

    public Map<String, List<Map<String, String>>> buildCategoryTree() {
        List<ExpenseSubcategory> subs = subcategoryRepository.findAllByOrderByDisplayOrderAsc();
        Map<String, List<Map<String, String>>> tree = new LinkedHashMap<>();
        for (ExpenseCategory c : categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()) {
            tree.put(c.getCode(), new ArrayList<>());
        }
        for (ExpenseSubcategory s : subs) {
            if (s.getCategory() == null || !Boolean.TRUE.equals(s.getActive())) continue;
            String catCode = s.getCategory().getCode();
            tree.computeIfAbsent(catCode, k -> new ArrayList<>());
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("value", s.getCode());
            entry.put("text", s.getName());
            if (s.getGroupLabel() != null && !s.getGroupLabel().isBlank()) {
                entry.put("group", s.getGroupLabel());
            }
            tree.get(catCode).add(entry);
        }
        return tree;
    }


    public String buildCategoryTreeJson() {
        try {
            return objectMapper.writeValueAsString(buildCategoryTree());
        } catch (Exception e) {
            return "{}";
        }
    }

    public String buildCategoryNamesJson() {
        Map<String, String> names = categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .collect(Collectors.toMap(ExpenseCategory::getCode, ExpenseCategory::getName, (a, b) -> a, LinkedHashMap::new));
        try {
            return objectMapper.writeValueAsString(names);
        } catch (Exception e) {
            return "{}";
        }
    }
}
