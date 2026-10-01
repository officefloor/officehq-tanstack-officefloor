package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard — the home summary: the client count, the project count, what is outstanding
 * kept SEPARATE per currency (the sum of every SENT invoice amount, grouped by the billing currency
 * of the invoice's client — money actually owed, never added across currencies) and the overdue
 * count (how many SENT invoices are past their due date). An invoice counts towards both only once it
 * has been sent:
 * DRAFTs have not gone out yet and PAID invoices are already settled, so both are excluded. "Overdue"
 * is measured against a reference date — the seeded {@link DashboardReference} if present (so the
 * harness is deterministic), otherwise today. A cross-entity aggregate read, so it injects the
 * repositories it needs. Wired by officefloor/rest/api/dashboard.GET.yml.
 */
public class GetDashboard {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, DashboardReferenceRepository reference,
            ObjectResponse<DashboardView> response) {
        // Each project belongs to a client, and each client is billed in their own currency. What is
        // owed is kept SEPARATE per currency — money is never added across currencies.
        Map<Long, String> currencyByProject = new java.util.HashMap<>();
        Map<Long, String> currencyByClient = clients.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Client::getId, Client::getCurrency));
        for (Project project : projects.findAll()) {
            currencyByProject.put(project.getId(),
                    currencyByClient.getOrDefault(project.getClientId(), "USD"));
        }
        // A sorted map so the per-currency figures come back in a stable (currency-code) order.
        Map<String, BigDecimal> outstandingByCurrency = new TreeMap<>();
        for (Invoice invoice : invoices.findAll()) {
            if (!"SENT".equals(invoice.getStatus())) {
                continue;
            }
            String currency = currencyByProject.getOrDefault(invoice.getProjectId(), "USD");
            outstandingByCurrency.merge(currency, invoice.getTotal(), BigDecimal::add);
        }
        List<CurrencyAmount> outstanding = outstandingByCurrency.entrySet().stream()
                .map(e -> new CurrencyAmount(e.getKey(), e.getValue()))
                .toList();
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
