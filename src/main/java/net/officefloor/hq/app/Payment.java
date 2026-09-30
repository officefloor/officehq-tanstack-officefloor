package net.officefloor.hq.app;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A payment a client has made against an {@link Invoice}: an amount and the date it was paid, plus
 * the id of the invoice it belongs to. Persisted to the {@code invoice_payments} table (Flyway
 * V16). An invoice may have many payments (partial payments over time); the date is an ISO date
 * string (YYYY-MM-DD) so the value shown is exactly what was stored.
 */
@Entity
@Table(name = "invoice_payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    private BigDecimal amount;

    @Column(name = "paid_on")
    private String date;

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
