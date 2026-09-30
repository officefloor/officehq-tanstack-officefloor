package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * An invoice as a project's list shows it: every stored {@link Invoice} field plus the money already
 * settled against it ({@code paid}, the sum of its {@link Payment}s) and how much is still owed
 * ({@code due} = amount - paid). The paid/due split is derived per request in {@link InvoicesGet}
 * rather than stored, so it always reflects the current payments. Additive over the raw invoice
 * shape — existing readers keep using {@code amount}; the new ones read {@code due}.
 */
public class ProjectInvoice {

    private final Long id;
    private final Long projectId;
    private final BigDecimal amount;
    private final String status;
    private final String issuedDate;
    private final String dueDate;
    private final BigDecimal paid;
    private final BigDecimal due;

    public ProjectInvoice(Invoice invoice, BigDecimal paid) {
        this.id = invoice.getId();
        this.projectId = invoice.getProjectId();
        this.amount = invoice.getAmount();
        this.status = invoice.getStatus();
        this.issuedDate = invoice.getIssuedDate();
        this.dueDate = invoice.getDueDate();
        this.paid = paid;
        this.due = invoice.getAmount().subtract(paid);
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

    public String getStatus() {
        return status;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getDue() {
        return due;
    }
}
