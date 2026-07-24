package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.dto.CostTableCreateRequest;
import com.alfatahi.erp.cutplan.dto.CostTableResponse;
import com.alfatahi.erp.cutplan.dto.CostTableUpdateRequest;
import com.alfatahi.erp.cutplan.entity.CostTable;
import com.alfatahi.erp.cutplan.entity.CostTableHistory;
import com.alfatahi.erp.cutplan.mapper.CostTableMapper;
import com.alfatahi.erp.cutplan.repository.CostTableHistoryRepository;
import com.alfatahi.erp.cutplan.repository.CostTableRepository;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CostTableService - Serviço de gerenciamento de tabelas de preço
 *
 * Responsabilidade:
 * - CRUD de tabelas de preço
 * - Gerenciar versionamento de preços
 * - Manter histórico de mudanças
 * - Buscar preços vigentes
 * - Análise de variação de preços
 * - Auditoria de alterações
 *
 * Fluxo de Atualização de Preço:
 * 1. Buscar preço atual
 * 2. Registrar novo preço com data de vigência
 * 3. Marcar preço antigo como expirado (effectiveTo)
 * 4. Registrar entrada em CostTableHistory
 * 5. Notificar (futuro) mudança de preço importante
 *
 * Exemplo:
 * - 01/01/2024: Preço vidro 8mm = R$ 150 (effectiveFrom: 01/01, effectiveTo: null)
 * - 15/03/2024: Preço aumenta para R$ 165
 *   → Atualizar preço antigo: effectiveTo = 14/03
 *   → Criar novo registro: R$ 165, effectiveFrom = 15/03, effectiveTo = null
 *   → Registrar em histórico: motivo, usuário, data
 */
@Slf4j
@Service
@Transactional
public class CostTableService {

    @Autowired
    private CostTableRepository costTableRepository;

    @Autowired
    private CostTableHistoryRepository costTableHistoryRepository;

    @Autowired
    private CostTableMapper costTableMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    /**
     * Criar nova entrada de tabela de preço
     */
    public CostTableResponse create(CostTableCreateRequest request, UUID userId) {
        log.info("Criando nova entrada de preço: {} - {}", request.getCategory(), request.getItemType());

        AppUser createdBy = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Fornecedor não encontrado: " + request.getSupplierId()
                    ));
        }

        LocalDate effectiveFrom = LocalDate.parse(request.getEffectiveFrom());
        LocalDate effectiveTo = request.getEffectiveTo() != null ?
                LocalDate.parse(request.getEffectiveTo()) : null;

        CostTable costTable = CostTable.builder()
                .category(request.getCategory())
                .itemType(request.getItemType())
                .description(request.getDescription())
                .unitPrice(request.getUnitPrice())
                .unit(request.getUnit())
                .supplier(supplier)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .active(true)
                .createdBy(createdBy)
                .remarks(request.getRemarks())
                .build();

        costTable = costTableRepository.save(costTable);

        log.info("Entrada de preço criada: ID={}, R$ {}", costTable.getId(), costTable.getUnitPrice());

        return costTableMapper.toResponse(costTable);
    }

    /**
     * Atualizar preço (cria nova versão, não sobrescreve)
     *
     * Este método implementa o versionamento:
     * 1. Busca preço vigente atual
     * 2. Marca como expirado (effectiveTo = hoje - 1)
     * 3. Cria novo registro com novo preço
     * 4. Registra mudança no histórico
     */
    public CostTableResponse updatePrice(UUID id, CostTableUpdateRequest request, UUID userId) {
        log.info("Atualizando preço: {}", id);

        CostTable oldPrice = costTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preço não encontrado: " + id));

        AppUser updatedBy = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        BigDecimal oldValue = oldPrice.getUnitPrice();
        BigDecimal newValue = request.getUnitPrice();

        // 1. Marcar preço antigo como expirado
        oldPrice.setEffectiveTo(LocalDate.now().minusDays(1));
        costTableRepository.save(oldPrice);

        log.debug("Preço antigo marcado como expirado");

        // 2. Criar novo preço
        LocalDate effectiveTo = request.getEffectiveTo() != null ?
                LocalDate.parse(request.getEffectiveTo()) : null;

        CostTable newPrice = CostTable.builder()
                .category(oldPrice.getCategory())
                .itemType(oldPrice.getItemType())
                .description(oldPrice.getDescription())
                .unitPrice(newValue)
                .unit(oldPrice.getUnit())
                .supplier(oldPrice.getSupplier())
                .effectiveFrom(LocalDate.now())
                .effectiveTo(effectiveTo)
                .active(true)
                .createdBy(updatedBy)
                .remarks(oldPrice.getRemarks())
                .build();

        newPrice = costTableRepository.save(newPrice);

        // 3. Registrar no histórico
        CostTableHistory history = CostTableHistory.builder()
                .costTable(newPrice)
                .oldPrice(oldValue)
                .newPrice(newValue)
                .changedBy(updatedBy)
                .reason(request.getReason())
                .reference(request.getReference())
                .build();

        costTableHistoryRepository.save(history);

        BigDecimal percentChange = newValue
                .subtract(oldValue)
                .divide(oldValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        log.info("Preço atualizado: R$ {} → R$ {} ({:+.2f}%)",
                oldValue, newValue, percentChange);

        return costTableMapper.toResponse(newPrice);
    }

    /**
     * Buscar preço por ID
     */
    @Transactional(readOnly = true)
    public CostTableResponse getById(UUID id) {
        log.debug("Buscando preço: {}", id);

        CostTable costTable = costTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preço não encontrado: " + id));

        return costTableMapper.toResponse(costTable);
    }

    /**
     * Listar preços com paginação
     */
    @Transactional(readOnly = true)
    public Page<CostTableResponse> listAll(Pageable pageable) {
        log.debug("Listando preços, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<CostTable> page = costTableRepository.findAll(pageable);
        return page.map(costTableMapper::toResponse);
    }

    /**
     * Listar preços por categoria
     */
    @Transactional(readOnly = true)
    public Page<CostTableResponse> listByCategory(String category, Pageable pageable) {
        log.debug("Listando preços da categoria: {}", category);

        Page<CostTable> page = costTableRepository.findByCategory(category, pageable);
        return page.map(costTableMapper::toResponse);
    }

    /**
     * Buscar preço vigente de um item específico
     */
    @Transactional(readOnly = true)
    public CostTableResponse getCurrentPrice(String category, String itemType) {
        log.debug("Buscando preço vigente: {} - {}", category, itemType);

        CostTable costTable = costTableRepository.findCurrentPrice(category, itemType)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Preço vigente não encontrado: %s - %s", category, itemType)
                ));

        return costTableMapper.toResponse(costTable);
    }

    /**
     * Buscar preço para uma data específica (histórico)
     */
    @Transactional(readOnly = true)
    public CostTableResponse getPriceAtDate(String category, String itemType, LocalDate date) {
        log.debug("Buscando preço para {} de {}: {} - {}",
                date, date.getDayOfWeek(), category, itemType);

        CostTable costTable = costTableRepository.findPriceAtDate(category, itemType, date)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Preço não encontrado para %s: %s - %s", date, category, itemType)
                ));

        return costTableMapper.toResponse(costTable);
    }

    /**
     * Listar histórico de preços de um item
     */
    @Transactional(readOnly = true)
    public List<CostTableResponse> getPriceHistory(String category, String itemType) {
        log.debug("Buscando histórico de preços: {} - {}", category, itemType);

        List<CostTable> history = costTableRepository.findPriceHistory(category, itemType);

        return history.stream()
                .map(costTableMapper::toResponse)
                .toList();
    }

    /**
     * Buscar preços de um fornecedor específico
     */
    @Transactional(readOnly = true)
    public List<CostTableResponse> getPricesBySupplier(UUID supplierId) {
        log.debug("Buscando preços do fornecedor: {}", supplierId);

        List<CostTable> prices = costTableRepository.findBySupplierIdAndActiveTrue(supplierId);

        return prices.stream()
                .map(costTableMapper::toResponse)
                .toList();
    }

    /**
     * Listar preços que expiram em breve
     */
    @Transactional(readOnly = true)
    public List<CostTableResponse> getSoonToExpire() {
        log.debug("Buscando preços que expiram em breve (próximos 7 dias)");

        List<CostTable> prices = costTableRepository.findSoonToExpire();

        return prices.stream()
                .map(costTableMapper::toResponse)
                .toList();
    }

    /**
     * Obter histórico de mudanças de um preço
     */
    @Transactional(readOnly = true)
    public List<CostTableHistory> getPriceChangeHistory(UUID costTableId) {
        log.debug("Buscando histórico de mudanças: {}", costTableId);

        return costTableHistoryRepository.findByCostTableIdOrderByChangedAtDesc(costTableId);
    }

    /**
     * Deletar preço (soft delete via campo active)
     */
    public void deletePrice(UUID id) {
        log.info("Deletando preço: {}", id);

        CostTable costTable = costTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preço não encontrado: " + id));

        costTable.setActive(false);
        costTableRepository.save(costTable);

        log.info("Preço deletado (soft delete): {}", id);
    }

    /**
     * Reativar preço deletado
     */
    public void reactivatePrice(UUID id) {
        log.info("Reativando preço: {}", id);

        CostTable costTable = costTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Preço não encontrado: " + id));

        costTable.setActive(true);
        costTableRepository.save(costTable);

        log.info("Preço reativado: {}", id);
    }

    /**
     * Análise: Calcular variação média de preço
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageVariationPercent(UUID costTableId) {
        log.debug("Calculando variação média para preço: {}", costTableId);

        return costTableHistoryRepository.getAverageVariationPercent(costTableId);
    }

    /**
     * Análise: Obter maior aumento registrado
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxIncreasePercent(UUID costTableId) {
        return costTableHistoryRepository.getMaxIncreasePercent(costTableId);
    }

    /**
     * Análise: Obter maior redução registrada
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxDecreasePercent(UUID costTableId) {
        return costTableHistoryRepository.getMaxDecreasePercent(costTableId);
    }

    /**
     * Importar preços em lote (CSV)
     *
     * Formato CSV esperado:
     * category,itemType,description,unitPrice,unit,supplier,effectiveFrom,remarks
     */
    public void importPricesFromCSV(String csvData, UUID userId) {
        log.info("Importando preços de CSV");

        AppUser importedBy = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userId));

        String[] lines = csvData.split("\n");
        int imported = 0;

        // Ignorar cabeçalho
        for (int i = 1; i < lines.length; i++) {
            try {
                String[] fields = lines[i].split(",");

                if (fields.length < 8) continue;

                CostTable costTable = CostTable.builder()
                        .category(fields[0].trim())
                        .itemType(fields[1].trim())
                        .description(fields[2].trim())
                        .unitPrice(new BigDecimal(fields[3].trim()))
                        .unit(fields[4].trim())
                        .effectiveFrom(LocalDate.parse(fields[6].trim()))
                        .active(true)
                        .createdBy(importedBy)
                        .remarks(fields[7].trim())
                        .build();

                costTableRepository.save(costTable);
                imported++;

            } catch (Exception e) {
                log.warn("Erro ao importar linha {}: {}", i, e.getMessage());
            }
        }

        log.info("Importação concluída: {} preços importados", imported);
    }

    /**
     * Exportar preços em CSV
     */
    public String exportPricesAsCSV(String category) {
        log.debug("Exportando preços da categoria {} como CSV", category);

        List<CostTable> prices = costTableRepository.findActiveByCategoryAndActiveTrue(category);

        StringBuilder csv = new StringBuilder();
        csv.append("Categoria,Tipo,Descrição,Preço,Unidade,Fornecedor,Vigente De,Vigente Até\n");

        for (CostTable price : prices) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    price.getCategory(),
                    price.getItemType(),
                    price.getDescription() != null ? price.getDescription() : "",
                    price.getUnitPrice(),
                    price.getUnit(),
                    price.getSupplier() != null ? price.getSupplier().getName() : "N/A",
                    price.getEffectiveFrom(),
                    price.getEffectiveTo() != null ? price.getEffectiveTo() : "Vigente"
            ));
        }

        return csv.toString();
    }

    /**
     * Análise: Comparar preços entre fornecedores
     */
    @Transactional(readOnly = true)
    public List<Object[]> compareSupplierPrices(String category, String itemType) {
        log.debug("Comparando preços entre fornecedores: {} - {}", category, itemType);

        // Será implementado usando query customizada
        return List.of();
    }
}