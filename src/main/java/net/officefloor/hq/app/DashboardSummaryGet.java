package net.officefloor.hq.app;

import java.time.LocalDate;
import java.time.ZoneOffset;

import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard/summary — the home dashboard's aggregate: the client and project counts, the
 * outstanding (unpaid) total, and how many SENT invoices are overdue. Reads across the repositories
 * so the front-end renders the whole picture in one request. Overdue is measured against the
 * {@code asOf} reference date in {@code app_settings} when one is seeded (making tests
 * deterministic), otherwise against today. Wired by
 * {@code officefloor/rest/api/dashboard/summary.GET.yml}.
 */
public class DashboardSummaryGet {

    private static final String AS_OF_KEY = "asOf";

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, AppSettingRepository settings,
            ObjectResponse<DashboardSummary> response) {
        String asOf = settings.findById(AS_OF_KEY).map(AppSetting::getValue)
                .orElseGet(() -> LocalDate.now(ZoneOffset.UTC).toString());
        response.send(new DashboardSummary(clients.count(), projects.count(),
                invoices.sumOutstanding(), invoices.countOverdue(asOf)));
    }
}
