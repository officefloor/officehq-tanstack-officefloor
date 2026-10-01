package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/projects/budget?projectId=&lt;id&gt; — a project's budget, how much has been invoiced
 * against it, and what is left. Scoped to one project, so the project id arrives as a query
 * parameter. The budget is what the user set on the project (treated as zero until one is set);
 * the invoiced figure is the sum of the amounts of the project's invoices that have gone out (SENT
 * or PAID — a DRAFT has not been invoiced yet); what is left is budget minus invoiced. Derived on
 * the server in {@link BigDecimal} so there is no float drift. Wired by
 * officefloor/rest/api/projects/budget.GET.yml.
 */
public class GetProjectBudget {

    public void service(@RequestParam("projectId") String projectId,
            ProjectRepository projects, InvoiceRepository invoices,
            ObjectResponse<ProjectBudgetView> response) {
        Long id = Long.valueOf(projectId);
        Project project = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid project is required"));
        BigDecimal budget = project.getBudget() == null ? BigDecimal.ZERO : project.getBudget();
        BigDecimal invoiced = invoices.findByProjectIdOrderByIdAsc(id).stream()
                .filter(invoice -> !"DRAFT".equals(invoice.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = budget.subtract(invoiced);
        response.send(new ProjectBudgetView(id, budget, invoiced, remaining));
    }
}
