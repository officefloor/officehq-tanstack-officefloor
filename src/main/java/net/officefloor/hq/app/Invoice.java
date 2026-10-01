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
 * An invoice the user raises against a project: an amount and the id of the project it is for. Maps
 * the {@code invoices} table (Flyway V4__invoices.sql); the id is IDENTITY-generated on create. The
 * amount is a {@link BigDecimal} because it is money (fixed scale, no float drift).
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * The lifecycle status of the invoice: DRAFT when raised, SENT once the user sends it, PAID once
     * the user takes payment. Payment is only allowed after it has been sent.
     */
    @Column(nullable = false)
    private String status;

    /** When the invoice went out. */
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    /** When the invoice is due to be paid. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * The percentage taken off the invoice as a discount (0–100). The subtotal is the sum of the
     * invoice's line items; the discount is that percentage of the subtotal, and the final total is
     * the subtotal minus the discount. Defaults to zero — no discount takes nothing off
     * (V29__invoice_discount.sql).
     */
    @Column(name = "discount_pct", nullable = false)
    private BigDecimal discountPct = BigDecimal.ZERO;

    protected Invoice() {
    }

    public Invoice(Long projectId, BigDecimal amount, LocalDate issuedDate, LocalDate dueDate) {
        this.projectId = projectId;
        this.amount = amount;
        this.status = "DRAFT";
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Set this invoice's amount to the sum of its line items. The amount is derived from the lines
     * (V15__invoice_line_items.sql), so it is recomputed and stored whenever a line is added.
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /** The percentage discount taken off this invoice's subtotal (0–100, zero when none is set). */
    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    /** Send this invoice — the DRAFT -> SENT transition the send action performs. */
    public void markSent() {
        this.status = "SENT";
    }

    /** Whether this invoice has been sent, and so may be paid. */
    public boolean isSent() {
        return "SENT".equals(this.status);
    }

    /** Mark this invoice paid — the status transition the pay action performs. */
    public void markPaid() {
        this.status = "PAID";
    }

    /**
     * Void this invoice — the terminal transition the cancel action performs when an invoice was
     * sent by mistake. A VOID invoice no longer counts towards what is owed.
     */
    public void markVoid() {
        this.status = "VOID";
    }
}
