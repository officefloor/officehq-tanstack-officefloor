package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/invoices}: the fields the user supplies when adding an invoice —
 * the id of the project it is for and its amount. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
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
