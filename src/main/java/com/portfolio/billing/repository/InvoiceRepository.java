package com.portfolio.billing.repository;

import com.portfolio.billing.domain.Invoice;
import com.portfolio.billing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
    List<Invoice> findAllByStatus(InvoiceStatus status);
    List<Invoice> findAllByCustomerId(Long customerId);
}
