package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * An invoice as the all-invoices list shows it: its own id, amount and lifecycle status plus the
 * NAME of the project it was raised against (not just the id), so the front-end renders the
 * cross-entity join without a second request. Built by the JPQL constructor expression in
 * {@link InvoiceRepository#findAllViews()}.
 */
public class InvoiceView {

    private final Long id;
    private final Long projectId;
    private final String projectName;
    private final BigDecimal amount;
    private final String status;

    public InvoiceView(Long id, Long projectId, String projectName, BigDecimal amount,
            String status) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
