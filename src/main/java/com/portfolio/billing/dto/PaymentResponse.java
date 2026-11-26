package com.portfolio.billing.dto;

import com.portfolio.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long paymentId,
        Long invoiceId,
        String idempotencyKey,
        BigDecimal amount,
        BigDecimal newPaidAmount,
        InvoiceStatus invoiceStatus,
        OffsetDateTime paidAt,
        String note
) {}
