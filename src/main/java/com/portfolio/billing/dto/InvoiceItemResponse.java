package com.portfolio.billing.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
