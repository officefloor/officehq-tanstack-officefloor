package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/projects/budget?projectId=<id>} — a project's budget standing: the agreed BUDGET
 * (Flyway V22), how much has been INVOICED against it (the sum of the amounts of that project's
 * invoices — the same worked-out figure a project's invoice list totals), and what is LEFT (budget
 * minus invoiced). Derived on the server so the money math lives in one place. When the project has
 * no budget set, budget and remaining come back null. Wired by
 * {@code officefloor/rest/api/projects/budget.GET.yml}.
 */
public class ProjectBudgetGetLogic {

    public void service(@RequestParam("projectId") Long projectId, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<ProjectBudgetView> response) {
        Project project = projects.findById(projectId).orElseThrow();
        BigDecimal budget = project.getBudget();
        List<Invoice> projectInvoices = invoices.findByProjectIdOrderByIdAsc(projectId);
        BigDecimal invoiced = projectInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = budget != null ? budget.subtract(invoiced) : null;
        response.send(new ProjectBudgetView(projectId, budget, invoiced, remaining));
    }

    /** A project's budget standing: its budget, the amount invoiced against it, and what is left. */
    public static class ProjectBudgetView {
        private final long projectId;
        private final BigDecimal budget;
        private final BigDecimal invoiced;
        private final BigDecimal remaining;

        public ProjectBudgetView(long projectId, BigDecimal budget, BigDecimal invoiced,
                BigDecimal remaining) {
            this.projectId = projectId;
            this.budget = budget;
            this.invoiced = invoiced;
            this.remaining = remaining;
        }

        public long getProjectId() {
            return projectId;
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
}
