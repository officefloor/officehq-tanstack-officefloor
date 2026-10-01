package net.officefloor.hq.app;

import java.math.BigDecimal;

/** The request body for setting a project's budget: which project, and the planned spend to set. */
public class SetProjectBudgetForm {

    private Long id;
    private BigDecimal budget;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
}
