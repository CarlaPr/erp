package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    @Query(value = "SELECT nextval('receipt_number_seq')", nativeQuery = true)
    Long nextReceiptSequence();

    @Query("SELECT r FROM Receipt r LEFT JOIN FETCH r.client LEFT JOIN FETCH r.workOrder " +
            "ORDER BY r.issueDate DESC, r.createdAt DESC")
    List<Receipt> findAllForListing();

    Optional<Receipt> findByPublicToken(String token);

    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.workOrder.id = :workOrderId")
    long countByWorkOrderId(@Param("workOrderId") UUID workOrderId);
}
