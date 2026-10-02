package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the cross-feature summary the home dashboard shows: how many clients
 * and projects exist, and how much money is still owed (the sum of SENT invoice amounts across all
 * projects — only invoices that have actually been sent count; DRAFT and PAID are excluded). Wired by
 * {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGetLogic {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardView> response) {
        // "Owed" is the net total after any discount (Flyway V27), worked out the one-place way
        // (InvoiceMoney) so the home figure agrees with the invoice detail and the client statement.
        BigDecimal outstanding = invoices.findAll().stream()
                .filter(i -> "SENT".equals(i.getStatus()))
                .map(i -> InvoiceMoney.netTotal(i.getAmount(), i.getDiscountPct()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.send(new DashboardView(clients.count(), projects.count(), outstanding));
    }

    /** The counts and outstanding total the dashboard renders. */
    public static class DashboardView {
        private final long clientsCount;
        private final long projectsCount;
        private final BigDecimal outstandingTotal;

        public DashboardView(long clientsCount, long projectsCount, BigDecimal outstandingTotal) {
            this.clientsCount = clientsCount;
            this.projectsCount = projectsCount;
            this.outstandingTotal = outstandingTotal;
        }

        public long getClientsCount() {
            return clientsCount;
        }

        public long getProjectsCount() {
            return projectsCount;
        }

        public BigDecimal getOutstandingTotal() {
            return outstandingTotal;
        }
    }
}
