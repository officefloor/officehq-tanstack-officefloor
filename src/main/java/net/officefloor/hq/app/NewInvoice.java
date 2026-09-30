package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for creating an invoice: the amount and the id of the project it is for (the
 * invoice id is generated). Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewInvoice {

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
