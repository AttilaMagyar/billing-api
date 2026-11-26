package com.portfolio.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
        @NotBlank @Size(max = 50) String invoiceNumber,
        @NotNull Long customerId,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotEmpty @Valid List<InvoiceItemRequest> items
) {}
