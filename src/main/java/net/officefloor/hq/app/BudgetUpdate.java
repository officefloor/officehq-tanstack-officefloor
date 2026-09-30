package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/projects/budget}: which project to set the budget on and the
 * budget amount to set. Bound from the JSON payload by the Spring MVC {@code @RequestBody} resolver.
 */
public class BudgetUpdate {

    private Long projectId;

    private BigDecimal budget;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
}
