package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One payment a client has made against an invoice: an amount received on a date. Maps the
 * {@code invoice_payments} table (Flyway V20__invoice_payments.sql); the id is IDENTITY-generated on
 * create. The amount is a {@link BigDecimal} because it is money (fixed scale, no float drift), and
 * the date is the day it was paid. An invoice may be paid in parts, so each payment is its own row.
 */
@Entity
@Table(name = "invoice_payments")
public class InvoicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_date", nullable = false)
    private LocalDate paidDate;

    protected InvoicePayment() {
    }

    public InvoicePayment(Long invoiceId, BigDecimal amount, LocalDate paidDate) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paidDate = paidDate;
    }

    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }
}
