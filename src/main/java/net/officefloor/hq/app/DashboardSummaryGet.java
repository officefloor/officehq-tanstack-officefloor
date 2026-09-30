package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
        // Which currency each project bills in, via its client — so a SENT invoice's owed amount is
        // added to the right currency bucket. Clients pay in different currencies and we never add
        // different currencies together (CLAUDE.md — the totals are kept separate per currency).
        Map<Long, String> clientCurrency = new HashMap<>();
        for (Client client : clients.findAll()) {
            clientCurrency.put(client.getId(), client.getCurrency());
        }
        Map<Long, String> projectCurrency = new HashMap<>();
        for (Project project : projects.findAll()) {
            projectCurrency.put(project.getId(),
                    clientCurrency.getOrDefault(project.getClientId(), "USD"));
        }
        BigDecimal outstanding = BigDecimal.ZERO;
        Map<String, BigDecimal> byCurrency = new TreeMap<>();
        for (Invoice invoice : invoices.findByStatus("SENT")) {
            BigDecimal owed =
                    ProjectInvoice.owedAmount(invoice.getAmount(), invoice.getDiscountPct());
            outstanding = outstanding.add(owed);
            String currency = projectCurrency.getOrDefault(invoice.getProjectId(), "USD");
            byCurrency.merge(currency, owed, BigDecimal::add);
        }
        response.send(new DashboardSummary(clients.count(), projects.count(),
                outstanding, byCurrency, invoices.countOverdue(asOf)));
    }
}
