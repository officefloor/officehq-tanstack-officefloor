package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard/summary — the home dashboard's aggregate: the client and project counts and
 * the outstanding (unpaid) total. Reads across all three repositories so the front-end renders the
 * whole picture in one request. Wired by {@code officefloor/rest/api/dashboard/summary.GET.yml}.
 */
public class DashboardSummaryGet {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardSummary> response) {
        response.send(new DashboardSummary(clients.count(), projects.count(),
                invoices.sumOutstanding()));
    }
}
