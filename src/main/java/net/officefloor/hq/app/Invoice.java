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
 * An invoice raised on a project: an amount, and the id of the project it bills. Persisted to the
 * {@code invoices} table (Flyway V4). The id is database-generated (IDENTITY) on create; the seed
 * path inserts explicit ids directly via JdbcTemplate (see {@link TestSupportController}).
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

    /** Lifecycle status: DRAFT (default, matching Flyway V9) -> SENT (once sent) -> PAID (once paid). */
    private String status = "DRAFT";

    /** The date the invoice went out (Flyway V7). Serialized as an ISO date, e.g. "2026-01-05". */
    @Column(name = "issued_date")
    private LocalDate issuedDate;

    /** The date the invoice is due (Flyway V7). Serialized as an ISO date, e.g. "2026-02-04". */
    @Column(name = "due_date")
    private LocalDate dueDate;

    public Invoice() {
    }

    public Invoice(Long projectId, BigDecimal amount) {
        this.projectId = projectId;
        this.amount = amount;
    }

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

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
