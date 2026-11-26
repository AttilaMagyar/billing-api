package com.portfolio.billing.service;

import com.portfolio.billing.domain.Customer;
import com.portfolio.billing.domain.Invoice;
import com.portfolio.billing.domain.InvoiceStatus;
import com.portfolio.billing.domain.Payment;
import com.portfolio.billing.dto.PaymentRequest;
import com.portfolio.billing.dto.PaymentResponse;
import com.portfolio.billing.exception.BusinessRuleException;
import com.portfolio.billing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentService paymentService;

    private Invoice issuedInvoice;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("test@example.com")
                .build();

        issuedInvoice = Invoice.builder()
                .id(10L)
                .invoiceNumber("INV-2026-001")
                .customer(customer)
                .status(InvoiceStatus.ISSUED)
                .issueDate(LocalDate.of(2026, 11, 1))
                .dueDate(LocalDate.of(2026, 11, 15))
                .totalAmount(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("0.00"))
                .currency("HUF")
                .build();
    }

    @Test
    void shouldMarkInvoicePaidWhenFullAmountIsPaid() {
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(invoiceService.getEntityById(10L)).thenReturn(issuedInvoice);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(
                10L,
                "key-1",
                new PaymentRequest(new BigDecimal("100.00"), "Full transfer")
        );

        assertEquals(InvoiceStatus.PAID, response.invoiceStatus());
        assertEquals(new BigDecimal("100.00"), response.newPaidAmount());
        assertEquals(99L, response.paymentId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentOnDuplicateIdempotencyKey() {
        Payment existingPayment = Payment.builder()
                .id(55L)
                .invoice(issuedInvoice)
                .idempotencyKey("dup-key")
                .amount(new BigDecimal("40.00"))
                .paidAt(OffsetDateTime.now())
                .note("Already processed")
                .build();
        issuedInvoice.setPaidAmount(new BigDecimal("40.00"));

        when(paymentRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.processPayment(
                10L,
                "dup-key",
                new PaymentRequest(new BigDecimal("40.00"), "Retry")
        );

        assertEquals(55L, response.paymentId());
        assertEquals(new BigDecimal("40.00"), response.newPaidAmount());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectOverpayment() {
        when(paymentRepository.findByIdempotencyKey("over-key")).thenReturn(Optional.empty());
        when(invoiceService.getEntityById(10L)).thenReturn(issuedInvoice);

        assertThrows(
                BusinessRuleException.class,
                () -> paymentService.processPayment(10L, "over-key", new PaymentRequest(new BigDecimal("150.00"), "Too much"))
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
