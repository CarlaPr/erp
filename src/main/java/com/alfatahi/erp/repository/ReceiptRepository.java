package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Receipt;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /**
     * Buscar recibo pelo número
     */
    Optional<Receipt> findByNumber(String number);

    /**
     * Buscar recibos por Conta a Receber
     */
    List<Receipt> findByAccountsReceivable(AccountsReceivable accountsReceivable);

    /**
     * Buscar recibos por Ordem de Serviço
     */
    List<Receipt> findByWorkOrder(WorkOrder workOrder);

    /**
     * Buscar recibos por Cliente
     */
    List<Receipt> findByClient(Client client);

    /**
     * Buscar recibos por Status
     */
    List<Receipt> findByStatus(String status);

    /**
     * Buscar recibos por intervalo de datas
     */
    @Query("SELECT r FROM Receipt r WHERE r.receiptDate BETWEEN :dateFrom AND :dateTo ORDER BY r.receiptDate DESC")
    List<Receipt> findByDateRange(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);

    /**
     * Contar recibos emitidos (status = issued, printed, sent, reissued)
     */
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.status IN ('issued', 'printed', 'sent', 'reissued')")
    long countIssuedReceipts();

    /**
     * Contar recibos pendentes (status = draft)
     */
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.status = 'draft'")
    long countPendingReceipts();

    /**
     * Listar todos os recibos ordenados por data desc
     */
    @Query("SELECT r FROM Receipt r ORDER BY r.receiptDate DESC, r.createdAt DESC")
    List<Receipt> findAllOrdered();

    /**
     * Contar recibos de um cliente
     */
    long countByClient(Client client);

    /**
     * Buscar recibos por intervalo de datas e status
     */
    @Query("SELECT r FROM Receipt r WHERE r.receiptDate BETWEEN :dateFrom AND :dateTo AND r.status = :status ORDER BY r.receiptDate DESC")
    List<Receipt> findByDateRangeAndStatus(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status") String status
    );

    /**
     * Buscar recibos por forma de pagamento
     */
    List<Receipt> findByPaymentMethod(String paymentMethod);

    /**
     * Verificar se existe recibo para uma conta a receber
     */
    boolean existsByAccountsReceivable(AccountsReceivable accountsReceivable);
}
