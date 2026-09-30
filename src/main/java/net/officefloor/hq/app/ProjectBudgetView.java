package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the /api/projects/budget routes return for one project: the {@code budget} planned for it, how
 * much has been {@code invoiced} against it (the sum of its invoices), and what is {@code remaining}
 * (budget minus invoiced). All three are money figures the project detail's budget panel shows.
 */
public class ProjectBudgetView {

    private final BigDecimal budget;
    private final BigDecimal invoiced;
    private final BigDecimal remaining;

    public ProjectBudgetView(BigDecimal budget, BigDecimal invoiced, BigDecimal remaining) {
        this.budget = budget;
        this.invoiced = invoiced;
        this.remaining = remaining;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getInvoiced() {
        return invoiced;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }
}
