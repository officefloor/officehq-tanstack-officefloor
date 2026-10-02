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
 * A payment a client has made against an invoice: an {@code amount} and the {@code date} it was
 * paid. Persisted to the {@code payments} table (Flyway V19) and belonging to one invoice
 * ({@code invoiceId} -&gt; invoices.id), so an invoice's detail page lists only its own payments.
 * The id is database-generated (IDENTITY) on create; the seed path inserts explicit ids directly
 * via JdbcTemplate (see {@link TestSupportController}).
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    private BigDecimal amount;

    /** The date the payment was made. Serialized as an ISO date, e.g. "2026-02-01". */
    @Column(name = "paid_date")
    private LocalDate date;

    public Payment() {
    }

    public Payment(Long invoiceId, BigDecimal amount, LocalDate date) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
