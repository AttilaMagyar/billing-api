package com.portfolio.billing.service;

import com.portfolio.billing.domain.Customer;
import com.portfolio.billing.domain.Invoice;
import com.portfolio.billing.domain.InvoiceItem;
import com.portfolio.billing.domain.InvoiceStatus;
import com.portfolio.billing.dto.CreateInvoiceRequest;
import com.portfolio.billing.dto.InvoiceItemRequest;
import com.portfolio.billing.dto.InvoiceItemResponse;
import com.portfolio.billing.dto.InvoiceResponse;
import com.portfolio.billing.exception.BusinessRuleException;
import com.portfolio.billing.exception.ResourceNotFoundException;
import com.portfolio.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        if (invoiceRepository.existsByInvoiceNumber(request.invoiceNumber())) {
            throw new BusinessRuleException("Invoice number already exists: " + request.invoiceNumber());
        }
        if (request.dueDate().isBefore(request.issueDate())) {
            throw new BusinessRuleException("Due date cannot be earlier than issue date");
        }

        Customer customer = customerService.getEntityById(request.customerId());

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.invoiceNumber().trim())
                .customer(customer)
                .status(InvoiceStatus.DRAFT)
                .issueDate(request.issueDate())
                .dueDate(request.dueDate())
                .currency(request.currency())
                .paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .createdAt(OffsetDateTime.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceItemRequest itemReq : request.items()) {
            BigDecimal lineTotal = itemReq.quantity()
                    .multiply(itemReq.unitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            InvoiceItem item = InvoiceItem.builder()
                    .description(itemReq.description().trim())
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice().setScale(2, RoundingMode.HALF_UP))
                    .lineTotal(lineTotal)
                    .build();

            invoice.addItem(item);
            total = total.add(lineTotal);
        }

        invoice.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved);
    }

    @Transactional
    public InvoiceResponse issue(Long invoiceId) {
        Invoice invoice = getEntityById(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be issued. Current status: " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Long invoiceId) {
        Invoice invoice = getEntityById(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot cancel invoice with status: " + invoice.getStatus());
        }
        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot cancel an invoice that already has payments");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(InvoiceStatus status) {
        List<Invoice> list = (status != null)
                ? invoiceRepository.findAllByStatus(status)
                : invoiceRepository.findAll();
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Invoice getEntityById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    public InvoiceResponse toResponse(Invoice inv) {
        BigDecimal remaining = inv.getTotalAmount().subtract(inv.getPaidAmount()).max(BigDecimal.ZERO);
        List<InvoiceItemResponse> items = inv.getItems().stream()
                .map(i -> new InvoiceItemResponse(i.getId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .toList();

        return new InvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getCustomer().getId(),
                inv.getCustomer().getName(),
                inv.getStatus(),
                inv.getIssueDate(),
                inv.getDueDate(),
                inv.getTotalAmount(),
                inv.getPaidAmount(),
                remaining,
                inv.getCurrency(),
                inv.getCreatedAt(),
                items
        );
    }
}
