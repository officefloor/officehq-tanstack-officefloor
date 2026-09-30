package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the home-screen summary across all three entities: the count of
 * clients, the count of projects, and the outstanding total (the money actually owed — the sum of
 * every SENT invoice's discounted amount, drafts and paid invoices excluded). Wired by
 * {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGet {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, AppSettingRepository settings,
            ObjectResponse<DashboardView> response) {
        // Outstanding is what is owed AFTER each invoice's discount, so the home screen agrees with
        // the invoice and statement: sum each SENT invoice's discounted amount (subtotal minus its
        // discount, see Invoice#getDiscountedAmount), the same discount the invoice-detail total uses.
        BigDecimal outstanding = BigDecimal.ZERO;
        for (Invoice invoice : invoices.findByStatusOrderByIdAsc("SENT")) {
            outstanding = outstanding.add(invoice.getDiscountedAmount());
        }
        // Overdue is measured against the seeded "asOf" reference date so the count is deterministic.
        // With no reference date set nothing is treated as overdue (count 0).
        long overdueCount = settings.findById("asOf")
                .map(s -> invoices.countSentOverdue(s.getValue()))
                .orElse(0L);
        response.send(new DashboardView(clients.count(), projects.count(),
                outstanding, overdueCount));
    }
}
