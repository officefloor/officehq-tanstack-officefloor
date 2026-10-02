package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/all} — every invoice across ALL projects, each carrying the NAME of the
 * project it bills and its lifecycle status, so one page can list them all in one place (the
 * per-project {@link InvoicesGetLogic} only lists a single project's). Ordered by id for a stable
 * list. Wired by {@code officefloor/rest/api/invoices/all.GET.yml}.
 */
public class AllInvoicesGetLogic {

    public void service(InvoiceRepository invoices, ProjectRepository projects,
            PaymentRepository payments, ObjectResponse<List<InvoiceView>> response) {
        Map<Long, String> nameByProject = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        List<InvoiceView> views = invoices.findAll().stream()
                .sorted(Comparator.comparing(Invoice::getId))
                .map(i -> {
                    BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(i.getId()).stream()
                            .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    String status = InvoiceStatus.derive(i.getStatus(), i.getAmount(), paid);
                    return new InvoiceView(i.getId(), i.getProjectId(),
                            nameByProject.get(i.getProjectId()), i.getAmount(), status);
                })
                .collect(Collectors.toList());
        response.send(views);
    }

    /** An invoice plus the name of the project it bills, as the all-invoices list needs it. */
    public static class InvoiceView {
        private final Long id;
        private final Long projectId;
        private final String projectName;
        private final BigDecimal amount;
        private final String status;

        public InvoiceView(Long id, Long projectId, String projectName, BigDecimal amount,
                String status) {
            this.id = id;
            this.projectId = projectId;
            this.projectName = projectName;
            this.amount = amount;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public Long getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }
    }
}
