package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for a project's budget: the planned spend set against it, how much has been
 * INVOICED against it (the sum of the amounts of invoices raised against the project that have gone
 * out), and what is LEFT (budget minus invoiced). The derivation lives on the server, computed in
 * {@link BigDecimal} so there is no float drift — the shape the project detail page renders into its
 * budget / invoiced / remaining figures.
 */
public record ProjectBudgetView(Long projectId, BigDecimal budget, BigDecimal invoiced,
        BigDecimal remaining) {
}
