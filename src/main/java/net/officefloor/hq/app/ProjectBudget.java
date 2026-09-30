package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * A project's budget picture: the {@code budget} planned for it, how much has been {@code invoiced}
 * against it (the sum of every invoice raised for the project), and what is {@code remaining}
 * (budget - invoiced). Derived per request in {@link ProjectBudgetGet} so it always reflects the
 * current budget and invoices. {@code budget} and {@code remaining} are null when no budget is set.
 */
public class ProjectBudget {

    private final BigDecimal budget;
    private final BigDecimal invoiced;
    private final BigDecimal remaining;

    public ProjectBudget(BigDecimal budget, BigDecimal invoiced) {
        this.budget = budget;
        this.invoiced = invoiced;
        this.remaining = budget == null ? null : budget.subtract(invoiced);
    }

    /** The money planned for the project; null if none is set. */
    public BigDecimal getBudget() {
        return budget;
    }

    /** How much has been invoiced against the project — the sum of its invoices' amounts. */
    public BigDecimal getInvoiced() {
        return invoiced;
    }

    /** What is left of the budget: budget - invoiced; null if no budget is set. */
    public BigDecimal getRemaining() {
        return remaining;
    }
}
