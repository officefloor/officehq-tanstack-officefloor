package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/projects/budget?projectId=N} — one project's budget summary: the budget set on it,
 * how much has been invoiced against it (the sum of every invoice raised against the project), and
 * what is left (budget minus invoiced). Wired by
 * {@code officefloor/rest/api/projects/budget.GET.yml}.
 *
 * <p>The invoiced total is derived here from the invoices (a server-side aggregate, mirroring the
 * dashboard's outstanding total), so the panel shows a single source of truth for what has been
 * billed against the project.
 */
public class ProjectBudgetGet {

    public void service(@HttpQueryParameter("projectId") String projectId,
            ProjectRepository projects, InvoiceRepository invoices,
            ObjectResponse<ProjectBudgetView> response) {
        Long id = Long.valueOf(projectId.trim());
        Project project = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("no project with id " + id));
        BigDecimal budget = project.getBudget();
        BigDecimal invoiced = invoices.sumAmountByProjectId(id);
        response.send(new ProjectBudgetView(budget, invoiced, budget.subtract(invoiced)));
    }
}
