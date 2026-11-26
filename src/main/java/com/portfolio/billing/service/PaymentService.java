package com.portfolio.billing.service;

import com.portfolio.billing.domain.Invoice;
import com.portfolio.billing.domain.InvoiceStatus;
import com.portfolio.billing.domain.Payment;
import com.portfolio.billing.dto.PaymentRequest;
import com.portfolio.billing.dto.PaymentResponse;
import com.portfolio.billing.exception.BusinessRuleException;
import com.portfolio.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public PaymentResponse processPayment(Long invoiceId, String idempotencyKey, PaymentRequest request) {
        String normalizedKey = idempotencyKey.trim();

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (!p.getInvoice().getId().equals(invoiceId)) {
                throw new BusinessRuleException("Idempotency-Key already used for a different invoice");
            }
            Invoice inv = p.getInvoice();
            return new PaymentResponse(
                    p.getId(),
                    inv.getId(),
                    p.getIdempotencyKey(),
                    p.getAmount(),
                    inv.getPaidAmount(),
                    inv.getStatus(),
                    p.getPaidAt(),
                    p.getNote()
            );
        }

        Invoice invoice = invoiceService.getEntityById(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.ISSUED && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessRuleException("Payments are only allowed for ISSUED or OVERDUE invoices. Status: " + invoice.getStatus());
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessRuleException("Payment amount (" + amount + ") exceeds remaining balance (" + remaining + ")");
        }

        BigDecimal updatedPaid = invoice.getPaidAmount().add(amount).setScale(2, RoundingMode.HALF_UP);
        invoice.setPaidAmount(updatedPaid);

        if (updatedPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .idempotencyKey(normalizedKey)
                .amount(amount)
                .paidAt(OffsetDateTime.now())
                .note(request.note())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                invoice.getId(),
                savedPayment.getIdempotencyKey(),
                savedPayment.getAmount(),
                invoice.getPaidAmount(),
                invoice.getStatus(),
                savedPayment.getPaidAt(),
                savedPayment.getNote()
        );
    }
}
