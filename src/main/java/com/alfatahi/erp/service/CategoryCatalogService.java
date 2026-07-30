package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.ExpenseCategory;
import com.alfatahi.erp.entity.ExpenseSubcategory;
import com.alfatahi.erp.repository.ExpenseCategoryRepository;
import com.alfatahi.erp.repository.ExpenseSubcategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fornece o catálogo de Categorias/Subcategorias de despesa (Contas a Pagar)
 * para as telas, incluindo a árvore em JSON usada pelos formulários para
 * popular a subcategoria dinamicamente a partir da categoria escolhida.
 */
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

    /**
     * Monta { "FIXA": [ {value,text,group}, ... ], "VARIAVEL": [...] }, usado tanto pelo
     * endpoint JSON consumido via fetch() nas telas quanto internamente.
     */
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

    /** Mesma árvore de {@link #buildCategoryTree()}, já serializada em JSON. */
    public String buildCategoryTreeJson() {
        try {
            return objectMapper.writeValueAsString(buildCategoryTree());
        } catch (Exception e) {
            return "{}";
        }
    }

    public String buildCategoryNamesJson() {
        Map<String, String> names = categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .collect(Collectors.toMap(ExpenseCategory::getCode, ExpenseCategory::getName, (a, b) -> a, LinkedHashMap::new));
        try {
            return objectMapper.writeValueAsString(names);
        } catch (Exception e) {
            return "{}";
        }
    }
}
