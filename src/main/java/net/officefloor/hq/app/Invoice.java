package net.officefloor.hq.app;

import java.math.BigDecimal;
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

    protected Invoice() {
    }

    public Invoice(Long projectId, BigDecimal amount) {
        this.projectId = projectId;
        this.amount = amount;
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
}
