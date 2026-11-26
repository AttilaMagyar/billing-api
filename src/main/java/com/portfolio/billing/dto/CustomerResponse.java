package com.portfolio.billing.dto;

import java.time.OffsetDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String taxNumber,
        OffsetDateTime createdAt
) {}
