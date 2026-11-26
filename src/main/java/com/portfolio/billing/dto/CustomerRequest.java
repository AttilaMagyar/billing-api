package com.portfolio.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(max = 40) String taxNumber
) {}
