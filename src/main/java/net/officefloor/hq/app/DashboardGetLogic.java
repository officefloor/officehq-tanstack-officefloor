package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the cross-feature summary the home dashboard shows: how many clients
 * and projects exist, and how much money is still owed (the sum of UNPAID invoice amounts across all
 * projects). Wired by {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGetLogic {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardView> response) {
        BigDecimal outstanding = invoices.findAll().stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .map(Invoice::getAmount)
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
