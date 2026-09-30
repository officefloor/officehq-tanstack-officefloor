package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects/{projectId}/budget — one project's budget picture: the budget planned for it,
 * how much has been invoiced against it (the sum of every invoice raised for the project), and what
 * is left (budget - invoiced). Derived per request so it always reflects the current invoices.
 * Wired by {@code officefloor/rest/api/projects/{projectId}/budget.GET.yml}.
 */
public class ProjectBudgetGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            ProjectRepository projects, InvoiceRepository invoices,
            ObjectResponse<ProjectBudget> response) {
        long id = Long.parseLong(projectId);
        BigDecimal budget = projects.findById(id).map(Project::getBudget).orElse(null);
        BigDecimal invoiced = BigDecimal.ZERO;
        for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(id)) {
            if (invoice.getAmount() != null) {
                invoiced = invoiced.add(invoice.getAmount());
            }
        }
        response.send(new ProjectBudget(budget, invoiced));
    }
}
