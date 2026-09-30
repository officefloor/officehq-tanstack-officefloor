package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the home-screen summary across all three entities: the count of
 * clients, the count of projects, and the outstanding total (the sum of every SENT invoice's
 * amount, i.e. the money actually owed — drafts and paid invoices are excluded). Wired by
 * {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGet {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, AppSettingRepository settings,
            ObjectResponse<DashboardView> response) {
        // Overdue is measured against the seeded "asOf" reference date so the count is deterministic.
        // With no reference date set nothing is treated as overdue (count 0).
        long overdueCount = settings.findById("asOf")
                .map(s -> invoices.countSentOverdue(s.getValue()))
                .orElse(0L);
        response.send(new DashboardView(clients.count(), projects.count(),
                invoices.sumSentAmount(), overdueCount));
    }
}
