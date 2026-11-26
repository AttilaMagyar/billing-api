package com.portfolio.billing.service;

import com.portfolio.billing.domain.Customer;
import com.portfolio.billing.dto.CustomerRequest;
import com.portfolio.billing.dto.CustomerResponse;
import com.portfolio.billing.exception.BusinessRuleException;
import com.portfolio.billing.exception.ResourceNotFoundException;
import com.portfolio.billing.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Customer with email already exists: " + request.email());
        }
        Customer customer = Customer.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .taxNumber(request.taxNumber())
                .createdAt(OffsetDateTime.now())
                .build();
        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Customer getEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getTaxNumber(), c.getCreatedAt());
    }
}
