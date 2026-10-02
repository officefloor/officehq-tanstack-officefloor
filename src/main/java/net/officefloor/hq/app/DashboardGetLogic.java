package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard} — the cross-feature summary the home dashboard shows: how many clients
 * and projects exist, and how much money is still owed (the sum of SENT invoice amounts across all
 * projects — only invoices that have actually been sent count; DRAFT and PAID are excluded). Because
 * different clients are billed in different currencies (Flyway V31), the outstanding figure is kept
 * SEPARATE PER CURRENCY and the currencies are never added together — one subtotal per currency,
 * each worked out from the SENT invoices of the clients billed in it (invoice -&gt; project -&gt;
 * client). Wired by {@code officefloor/rest/api/dashboard.GET.yml}.
 */
public class DashboardGetLogic {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, ObjectResponse<DashboardView> response) {
        // invoice -> project -> client -> currency, so each invoice's owed figure lands under the
        // currency its client is billed in.
        Map<Long, Long> projectToClient = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getClientId));
        Map<Long, String> clientCurrency = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getCurrency));
        // "Owed" is the net total after any discount (Flyway V27), worked out the one-place way
        // (InvoiceMoney) so the home figure agrees with the invoice detail and the client statement.
        // TreeMap keeps the currencies in a stable (alphabetical) order for the dashboard.
        Map<String, BigDecimal> byCurrency = new TreeMap<>();
        for (Invoice invoice : invoices.findAll()) {
            if (!"SENT".equals(invoice.getStatus())) {
                continue;
            }
            String currency = clientCurrency.getOrDefault(
                    projectToClient.get(invoice.getProjectId()), "USD");
            BigDecimal net = InvoiceMoney.netTotal(invoice.getAmount(), invoice.getDiscountPct(),
                    invoice.getTaxPct());
            byCurrency.merge(currency, net, BigDecimal::add);
        }
        List<OutstandingByCurrency> outstanding = byCurrency.entrySet().stream()
                .map(e -> new OutstandingByCurrency(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        response.send(new DashboardView(clients.count(), projects.count(), outstanding));
    }

    /** The counts and the per-currency outstanding totals the dashboard renders. */
    public static class DashboardView {
        private final long clientsCount;
        private final long projectsCount;
        private final List<OutstandingByCurrency> outstanding;

        public DashboardView(long clientsCount, long projectsCount,
                List<OutstandingByCurrency> outstanding) {
            this.clientsCount = clientsCount;
            this.projectsCount = projectsCount;
            this.outstanding = outstanding;
        }

        public long getClientsCount() {
            return clientsCount;
        }

        public long getProjectsCount() {
            return projectsCount;
        }

        public List<OutstandingByCurrency> getOutstanding() {
            return outstanding;
        }
    }

    /** How much is owed in one currency — never added to the other currencies' totals. */
    public static class OutstandingByCurrency {
        private final String currency;
        private final BigDecimal amount;

        public OutstandingByCurrency(String currency, BigDecimal amount) {
            this.currency = currency;
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
