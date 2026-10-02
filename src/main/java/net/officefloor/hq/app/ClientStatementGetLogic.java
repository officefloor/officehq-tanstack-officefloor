package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients/statement?clientId=<id>} — a client's statement: every invoice across ALL
 * of that client's projects gathered in one place, each carrying how much is still due on it (its
 * amount minus what has been paid against it — the same money rule as {@link InvoiceDueGetLogic}),
 * and the OUTSTANDING TOTAL the client still owes (the sum of those dues). Derived on the server so
 * the money math lives in one place. Wired by {@code officefloor/rest/api/clients/statement.GET.yml}.
 */
public class ClientStatementGetLogic {

    public void service(@RequestParam("clientId") Long clientId, ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<ClientStatementView> response) {
        // The client's projects are the ones its invoices hang off (invoice -> project -> client).
        List<Project> clientProjects = projects.findAll().stream()
                .filter(p -> clientId.equals(p.getClientId()))
                .collect(Collectors.toList());
        Set<Long> projectIds = clientProjects.stream().map(Project::getId).collect(Collectors.toSet());
        Map<Long, String> projectNames = clientProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        List<StatementInvoiceView> rows = invoices.findAll().stream()
                .filter(i -> projectIds.contains(i.getProjectId()))
                .sorted(Comparator.comparing(Invoice::getId))
                .map(i -> {
                    BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(i.getId()).stream()
                            .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    // What is owed is the net total after any discount (Flyway V27), worked out the
                    // one-place way (InvoiceMoney) so the statement agrees with the dashboard and the
                    // invoice detail. The due — and the status derived from it — follow that total.
                    BigDecimal netTotal = InvoiceMoney.netTotal(i.getAmount(), i.getDiscountPct(),
                            i.getTaxPct());
                    BigDecimal due = netTotal.subtract(paid);
                    String status = InvoiceStatus.derive(i.getStatus(), netTotal, paid);
                    return new StatementInvoiceView(i.getId(), i.getProjectId(), netTotal,
                            paid, due, status);
                })
                .collect(Collectors.toList());
        BigDecimal outstanding = rows.stream().map(StatementInvoiceView::getDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Group the invoices under the job (project) they belong to, preserving invoice order within
        // each group and group order by first appearance, and carry each job's subtotal (the sum of
        // its invoices' dues). The subtotals sum back to the same outstanding total.
        Map<Long, StatementProjectView> byProject = new LinkedHashMap<>();
        for (StatementInvoiceView row : rows) {
            byProject.computeIfAbsent(row.getProjectId(), id -> new StatementProjectView(id,
                    projectNames.getOrDefault(id, ""))).add(row);
        }
        List<StatementProjectView> groups = new ArrayList<>(byProject.values());
        response.send(new ClientStatementView(clientId, rows, groups, outstanding));
    }

    /** A client's statement: its invoices, grouped by job with subtotals, and the total still owed. */
    public static class ClientStatementView {
        private final long clientId;
        private final List<StatementInvoiceView> invoices;
        private final List<StatementProjectView> projects;
        private final BigDecimal outstandingTotal;

        public ClientStatementView(long clientId, List<StatementInvoiceView> invoices,
                List<StatementProjectView> projects, BigDecimal outstandingTotal) {
            this.clientId = clientId;
            this.invoices = invoices;
            this.projects = projects;
            this.outstandingTotal = outstandingTotal;
        }

        public long getClientId() {
            return clientId;
        }

        public List<StatementInvoiceView> getInvoices() {
            return invoices;
        }

        public List<StatementProjectView> getProjects() {
            return projects;
        }

        public BigDecimal getOutstandingTotal() {
            return outstandingTotal;
        }
    }

    /** One job (project) on the statement: its invoices and the subtotal still due across them. */
    public static class StatementProjectView {
        private final long projectId;
        private final String name;
        private final List<StatementInvoiceView> invoices = new ArrayList<>();
        private BigDecimal subtotal = BigDecimal.ZERO;

        public StatementProjectView(long projectId, String name) {
            this.projectId = projectId;
            this.name = name;
        }

        void add(StatementInvoiceView invoice) {
            this.invoices.add(invoice);
            this.subtotal = this.subtotal.add(invoice.getDue());
        }

        public long getProjectId() {
            return projectId;
        }

        public String getName() {
            return name;
        }

        public List<StatementInvoiceView> getInvoices() {
            return invoices;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }

    /** One invoice on the statement: its amount, what has been paid, what is still due, and status. */
    public static class StatementInvoiceView {
        private final long id;
        private final long projectId;
        private final BigDecimal amount;
        private final BigDecimal paid;
        private final BigDecimal due;
        private final String status;

        public StatementInvoiceView(long id, long projectId, BigDecimal amount, BigDecimal paid,
                BigDecimal due, String status) {
            this.id = id;
            this.projectId = projectId;
            this.amount = amount;
            this.paid = paid;
            this.due = due;
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public long getProjectId() {
            return projectId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getPaid() {
            return paid;
        }

        public BigDecimal getDue() {
            return due;
        }

        public String getStatus() {
            return status;
        }
    }
}
