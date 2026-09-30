package net.officefloor.hq.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared lookup for the currency a piece of money is shown in. Money hangs off clients, but the
 * things the app lists (invoices, line items) hang off projects, so the recurring need is "given a
 * project, which currency is its client billed in". Kept in one place so every endpoint that renders
 * an amount — {@link InvoicesGet}, {@link ClientStatementGet}, {@link DashboardGet},
 * {@link DashboardTopClientsGet}, {@link InvoicesAllGet}, {@link LineItemsGet},
 * {@link InvoiceSummaryGet} — resolves the currency the same way and never adds two currencies
 * together.
 */
final class Currencies {

    /** The currency assumed when a client has none recorded — the app's pre-existing default. */
    static final String DEFAULT = "USD";

    private Currencies() {
    }

    /** Currency code keyed by client id, defaulting to {@link #DEFAULT} for any client without one. */
    static Map<Long, String> byClient(ClientRepository clients) {
        Map<Long, String> byClient = new HashMap<>();
        for (Client client : clients.findAllByOrderByIdAsc()) {
            byClient.put(client.getId(), of(client.getCurrency()));
        }
        return byClient;
    }

    /**
     * Currency code keyed by project id: each project's client's currency, so an invoice or line item
     * (which knows its project) can be shown in the owning client's currency.
     */
    static Map<Long, String> byProject(ProjectRepository projects, ClientRepository clients) {
        Map<Long, String> byClient = byClient(clients);
        Map<Long, String> byProject = new HashMap<>();
        for (Project project : projects.findAllByOrderByIdAsc()) {
            byProject.put(project.getId(), byClient.getOrDefault(project.getClientId(), DEFAULT));
        }
        return byProject;
    }

    /** A currency code, or {@link #DEFAULT} when none is recorded. */
    static String of(String currency) {
        return currency == null || currency.isBlank() ? DEFAULT : currency;
    }
}
