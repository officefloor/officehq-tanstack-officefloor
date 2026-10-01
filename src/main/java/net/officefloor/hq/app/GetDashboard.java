package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard — the home summary: the client count, the project count, and the outstanding
 * total (the sum of every UNPAID invoice amount). A cross-entity aggregate read, so it injects all
 * three repositories. Wired by officefloor/rest/api/dashboard.GET.yml.
 */
public class GetDashboard {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardView> response) {
        BigDecimal outstanding = invoices.findAll().stream()
                .filter(invoice -> "UNPAID".equals(invoice.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.send(new DashboardView(clients.count(), projects.count(), outstanding));
    }
}
