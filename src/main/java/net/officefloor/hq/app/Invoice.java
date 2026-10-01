package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * The percentage sales tax added on top of the invoice (0–100), applied AFTER the discount. The
     * tax is that percentage of the discounted amount (subtotal minus discount), and the final total
     * is the discounted amount plus that tax. Defaults to zero — no tax set adds nothing on
     * (V30__invoice_tax.sql).
     */
    @Column(name = "tax_pct", nullable = false)
    private BigDecimal taxPct = BigDecimal.ZERO;

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

    /** The percentage sales tax added on top of this invoice (0–100, zero when none is set). */
    public BigDecimal getTaxPct() {
        return taxPct;
    }

    /**
     * The final total owed on this invoice: the subtotal ({@link #getAmount()}) minus the percentage
     * discount taken off it, then the percentage sales tax added on top of what is left. This is the
     * figure that counts as money owed everywhere the amount due is worked out — the dashboard
     * outstanding total, an invoice's remaining balance and the client statement — so the discount and
     * tax flow through consistently. Derived in {@link BigDecimal} at money scale (no float drift), the
     * same derivation {@link GetInvoiceSummary} applies to the detail view.
     */
    public BigDecimal getTotal() {
        BigDecimal base = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal discountPercent = discountPct == null ? BigDecimal.ZERO : discountPct;
        BigDecimal discount =
                base.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discounted = base.subtract(discount);
        BigDecimal taxPercent = taxPct == null ? BigDecimal.ZERO : taxPct;
        BigDecimal tax =
                discounted.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return discounted.add(tax);
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
