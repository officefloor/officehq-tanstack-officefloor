package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/budget — set (or change) the planned spend on a project, returning the updated
 * budget alongside how much has been invoiced against it and what is left. Wired by
 * officefloor/rest/api/projects/budget.POST.yml.
 *
 * We reject a missing or unknown project id, and a missing or negative budget, before writing
 * anything so a bad request never changes state. The invoiced / remaining figures are derived the
 * same way {@link GetProjectBudget} derives them, so the write returns a view the panel can render
 * directly.
 */
public class SetProjectBudget {

    public void service(@RequestBody SetProjectBudgetForm form, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<ProjectBudgetView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A project id is required");
        }
        BigDecimal budget = form.getBudget();
        if (budget == null || budget.signum() < 0) {
            throw new IllegalArgumentException("A valid budget is required");
        }
        Project project = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid project is required"));
        project.setBudget(budget);
        projects.save(project);
        BigDecimal invoiced = invoices.findByProjectIdOrderByIdAsc(id).stream()
                .filter(invoice -> !"DRAFT".equals(invoice.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.send(new ProjectBudgetView(id, budget, invoiced, budget.subtract(invoiced)));
    }
}
