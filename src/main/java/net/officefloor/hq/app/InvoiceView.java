package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the {@code /api/invoices/all} route returns: an invoice plus the NAME of the project it is
 * for, so the one-place invoices list shows the project's name (not its id) and its lifecycle
 * status without a second lookup. Built by joining {@link Invoice} against {@link Project} in
 * {@link InvoicesAllGet}, mirroring how {@link ProjectView} carries the client's name.
 */
public class InvoiceView {

    private final Long id;
    private final Long projectId;
    private final String projectName;
    private final BigDecimal amount;
    private final String status;
    private final String issuedDate;
    private final String dueDate;
    private final String currency;

    public InvoiceView(Long id, Long projectId, String projectName, BigDecimal amount,
            String status, String issuedDate, String dueDate, String currency) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.amount = amount;
        this.status = status;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.currency = currency;
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

    public String getIssuedDate() {
        return issuedDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    /** The currency this invoice is shown in (the billing currency of its project's client). */
    public String getCurrency() {
        return currency;
    }
}
