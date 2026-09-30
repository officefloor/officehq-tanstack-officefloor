package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
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
        // Clients are billed in different currencies, so the totals are kept SEPARATE per currency and
        // never added together — each SENT invoice adds to the bucket of its client's currency.
        Map<Long, String> currencyByProject = Currencies.byProject(projects, clients);
        Map<String, BigDecimal> outstandingByCurrency = new TreeMap<>();
        for (Invoice invoice : invoices.findByStatusOrderByIdAsc("SENT")) {
            String currency = currencyByProject.getOrDefault(invoice.getProjectId(),
                    Currencies.DEFAULT);
            outstandingByCurrency.merge(currency, invoice.getDiscountedAmount(), BigDecimal::add);
        }
        // Overdue is measured against the seeded "asOf" reference date so the count is deterministic.
        // With no reference date set nothing is treated as overdue (count 0).
        long overdueCount = settings.findById("asOf")
                .map(s -> invoices.countSentOverdue(s.getValue()))
                .orElse(0L);
        response.send(new DashboardView(clients.count(), projects.count(),
                outstandingByCurrency, overdueCount));
    }
}
