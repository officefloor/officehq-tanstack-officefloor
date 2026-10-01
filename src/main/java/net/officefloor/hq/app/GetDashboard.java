package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard — the home summary: the client count, the project count, the outstanding total
 * (the sum of every SENT invoice amount — money actually owed) and the overdue count (how many SENT
 * invoices are past their due date). An invoice counts towards both only once it has been sent:
 * DRAFTs have not gone out yet and PAID invoices are already settled, so both are excluded. "Overdue"
 * is measured against a reference date — the seeded {@link DashboardReference} if present (so the
 * harness is deterministic), otherwise today. A cross-entity aggregate read, so it injects the
 * repositories it needs. Wired by officefloor/rest/api/dashboard.GET.yml.
 */
public class GetDashboard {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, DashboardReferenceRepository reference,
            ObjectResponse<DashboardView> response) {
        BigDecimal outstanding = invoices.findAll().stream()
                .filter(invoice -> "SENT".equals(invoice.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate asOf = reference.findAll().stream()
                .findFirst()
                .map(DashboardReference::getAsOf)
                .orElseGet(LocalDate::now);
        long overdue = invoices.findAll().stream()
                .filter(invoice -> "SENT".equals(invoice.getStatus()))
                .filter(invoice -> invoice.getDueDate().isBefore(asOf))
                .count();
        response.send(new DashboardView(clients.count(), projects.count(), outstanding, overdue));
    }
}
