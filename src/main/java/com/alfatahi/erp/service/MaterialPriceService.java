package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.MaterialPriceHistory;
import com.alfatahi.erp.entity.MaterialPriceItem;
import com.alfatahi.erp.repository.MaterialPriceHistoryRepository;
import com.alfatahi.erp.repository.MaterialPriceItemRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class MaterialPriceService {

    private final MaterialPriceItemRepository itemRepository;
    private final MaterialPriceHistoryRepository historyRepository;

    public MaterialPriceService(MaterialPriceItemRepository itemRepository,
                                 MaterialPriceHistoryRepository historyRepository) {
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
    }

    public List<MaterialPriceItem> listAll() {
        return itemRepository.findAllByOrderByCategoryAscNameAsc();
    }

    public List<MaterialPriceItem> listByCategory(MaterialPriceItem.Category category) {
        return itemRepository.findByCategoryAndActiveTrueOrderByNameAsc(category);
    }

    /** Busca o preço vigente de um vidro pela especificação técnica (tipo/cor/acabamento/espessura). */
    public Optional<MaterialPriceItem> findCurrentGlassPrice(String glassType, String color, String finish, BigDecimal thickness) {
        LocalDate today = LocalDate.now();
        return itemRepository.findByCategoryAndActiveTrueAndGlassTypeAndColorAndFinishAndThickness(
                        MaterialPriceItem.Category.GLASS, glassType, color, finish, thickness)
                .stream()
                .filter(p -> p.getEffectiveDate() == null || !p.getEffectiveDate().isAfter(today))
                .filter(p -> p.getExpiryDate() == null || !p.getExpiryDate().isBefore(today))
                .findFirst();
    }

    @Transactional
    public MaterialPriceItem save(MaterialPriceItem item, String userLogin, String reason) {
        boolean isNew = item.getId() == null;
        MaterialPriceItem target;
        BigDecimal oldPrice = null;

        if (!isNew) {
            target = itemRepository.findById(item.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Insumo não encontrado"));
            oldPrice = target.getPrice();
        } else {
            target = new MaterialPriceItem();
        }

        target.setCategory(item.getCategory());
        target.setName(item.getName());
        target.setManufacturer(item.getManufacturer());
        target.setSupplier(item.getSupplier());
        target.setGlassType(item.getGlassType());
        target.setThickness(item.getThickness());
        target.setColor(item.getColor());
        target.setFinish(item.getFinish());
        target.setAluminumLine(item.getAluminumLine());
        target.setAluminumProfile(item.getAluminumProfile());
        target.setHardwareCategory(item.getHardwareCategory());
        target.setCode(item.getCode());
        target.setUnit(item.getUnit());
        target.setMinPrice(item.getMinPrice());
        target.setEffectiveDate(item.getEffectiveDate() != null ? item.getEffectiveDate() : LocalDate.now());
        target.setExpiryDate(item.getExpiryDate());
        target.setActive(item.getActive() != null ? item.getActive() : true);
        target.setNotes(item.getNotes());
        target.setUpdatedAt(java.time.LocalDateTime.now());

        boolean priceChanged = isNew || oldPrice == null || oldPrice.compareTo(item.getPrice()) != 0;
        target.setPrice(item.getPrice());

        MaterialPriceItem saved = itemRepository.save(target);

        if (priceChanged) {
            MaterialPriceHistory h = new MaterialPriceHistory();
            h.setPriceItem(saved);
            h.setOldPrice(oldPrice);
            h.setNewPrice(item.getPrice());
            h.setChangedBy(userLogin);
            h.setReason(isNew ? "Cadastro inicial" : reason);
            historyRepository.save(h);
        }

        return saved;
    }

    public void delete(UUID id) {
        itemRepository.deleteById(id);
    }

    public List<MaterialPriceHistory> history(UUID priceItemId) {
        return historyRepository.findByPriceItemIdOrderByChangedAtDesc(priceItemId);
    }

    /**
     * Importa/atualiza em lote a partir de uma planilha Excel (.xlsx) enviada pelo fornecedor.
     * Colunas esperadas (primeira linha = cabeçalho, ordem livre por nome):
     * categoria | nome | fabricante | tipo_vidro | espessura | cor | acabamento |
     * linha_aluminio | perfil_aluminio | categoria_ferragem | codigo | unidade | preco | preco_minimo
     */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file, String userLogin) throws Exception {
        ImportResult result = new ImportResult();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Planilha vazia");

            java.util.Map<String, Integer> colIndex = new java.util.HashMap<>();
            for (Cell c : header) {
                colIndex.put(normalize(c.getStringCellValue()), c.getColumnIndex());
            }

            DataFormatter fmt = new DataFormatter();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = getStr(row, colIndex, fmt, "nome");
                if (name == null || name.isBlank()) continue;

                try {
                    MaterialPriceItem item = new MaterialPriceItem();
                    item.setCategory(MaterialPriceItem.Category.valueOf(
                            getStr(row, colIndex, fmt, "categoria").trim().toUpperCase()));
                    item.setName(name);
                    item.setManufacturer(getStr(row, colIndex, fmt, "fabricante"));
                    item.setGlassType(getStr(row, colIndex, fmt, "tipo_vidro"));
                    item.setColor(getStr(row, colIndex, fmt, "cor"));
                    item.setFinish(getStr(row, colIndex, fmt, "acabamento"));
                    item.setAluminumLine(getStr(row, colIndex, fmt, "linha_aluminio"));
                    item.setAluminumProfile(getStr(row, colIndex, fmt, "perfil_aluminio"));
                    item.setHardwareCategory(getStr(row, colIndex, fmt, "categoria_ferragem"));
                    item.setCode(getStr(row, colIndex, fmt, "codigo"));
                    String thick = getStr(row, colIndex, fmt, "espessura");
                    if (thick != null && !thick.isBlank()) item.setThickness(new BigDecimal(thick.replace(",", ".")));
                    item.setUnit(MaterialPriceItem.Unit.valueOf(getStr(row, colIndex, fmt, "unidade").trim().toUpperCase()));
                    item.setPrice(new BigDecimal(getStr(row, colIndex, fmt, "preco").replace(",", ".")));
                    String minPrice = getStr(row, colIndex, fmt, "preco_minimo");
                    if (minPrice != null && !minPrice.isBlank()) item.setMinPrice(new BigDecimal(minPrice.replace(",", ".")));
                    item.setActive(true);

                    // Se já existir insumo com mesmo nome+categoria, atualiza (nova versão de preço); senão cria.
                    Optional<MaterialPriceItem> existing = itemRepository.findAllByOrderByCategoryAscNameAsc().stream()
                            .filter(e -> e.getName().equalsIgnoreCase(name) && e.getCategory() == item.getCategory())
                            .findFirst();
                    existing.ifPresent(e -> item.setId(e.getId()));

                    save(item, userLogin, "Importação em lote via planilha");
                    result.imported++;
                } catch (Exception rowEx) {
                    result.errors.add("Linha " + (r + 1) + ": " + rowEx.getMessage());
                }
            }
        }
        return result;
    }

    private String getStr(Row row, java.util.Map<String, Integer> colIndex, DataFormatter fmt, String col) {
        Integer idx = colIndex.get(col);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        return cell == null ? null : fmt.formatCellValue(cell).trim();
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase()
                .replace("ê", "e").replace("é", "e").replace("á", "a").replace("ã", "a").replace("í", "i").replace("ó", "o");
    }

    public static class ImportResult {
        public int imported = 0;
        public List<String> errors = new ArrayList<>();
    }
}
