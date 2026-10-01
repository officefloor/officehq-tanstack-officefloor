package net.officefloor.hq.app;

import java.math.BigDecimal;

/** The request body for creating an invoice: the amount typed and the id of the owning project. */
public class InvoiceForm {

    private Long projectId;
    private BigDecimal amount;

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
}
