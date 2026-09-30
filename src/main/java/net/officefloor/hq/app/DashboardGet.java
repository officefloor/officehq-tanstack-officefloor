package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the home-screen summary across all three entities: the count of
 * clients, the count of projects, and the outstanding total (the sum of every UNPAID invoice's
 * amount, i.e. the money still owed). Wired by {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGet {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardView> response) {
        response.send(new DashboardView(clients.count(), projects.count(),
                invoices.sumUnpaidAmount()));
    }
}
