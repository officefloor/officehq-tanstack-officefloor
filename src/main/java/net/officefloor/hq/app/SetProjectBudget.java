package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/projects/budget} — set the budget on a project and return its refreshed budget
 * summary (budget, invoiced, remaining). Wired by
 * {@code officefloor/rest/api/projects/budget.POST.yml}. The project must exist and the budget cannot
 * be negative.
 */
public class SetProjectBudget {

    public void service(@RequestBody BudgetUpdate body, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<ProjectBudgetView> response) {
        Long id = body.getProjectId();
        Project project = id == null ? null : projects.findById(id).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("setting a budget requires an existing project");
        }
        BigDecimal budget = body.getBudget() == null ? BigDecimal.ZERO : body.getBudget();
        if (budget.signum() < 0) {
            throw new IllegalArgumentException("a budget cannot be negative");
        }
        project.setBudget(budget);
        projects.save(project);
        BigDecimal invoiced = invoices.sumAmountByProjectId(id);
        response.send(new ProjectBudgetView(budget, invoiced, budget.subtract(invoiced)));
    }
}
