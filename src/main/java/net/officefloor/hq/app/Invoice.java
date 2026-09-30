package net.officefloor.hq.app;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An invoice raised against a {@link Project}: a monetary amount and the id of the project it
 * belongs to. Persisted to the {@code invoices} table (Flyway V4).
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    private BigDecimal amount;

    /** Lifecycle stage: 'DRAFT' (the default for a new invoice), then 'SENT', then 'PAID'. */
    private String status = "DRAFT";

    /** The date the invoice went out, as an ISO date string (YYYY-MM-DD). */
    @Column(name = "issued_date")
    private String issuedDate;

    /** The date the invoice is due, as an ISO date string (YYYY-MM-DD). */
    @Column(name = "due_date")
    private String dueDate;

    /**
     * The percentage taken off the invoice as a discount (0 = none). Applied to the subtotal (the
     * sum of the line items) to work out the invoice's final total; the line-item amounts themselves
     * are unchanged. Stored in the {@code discount_pct} column (Flyway V23).
     */
    @Column(name = "discount_pct")
    private BigDecimal discountPct = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct;
    }
}
