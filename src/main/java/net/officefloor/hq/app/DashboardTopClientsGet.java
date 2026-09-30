package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard/top-clients} — the home screen's top clients, ranked by how much each
 * still owes, most owed first, capped at five. Wired by
 * {@code officefloor/rest/api/dashboard/top-clients.GET.yml}.
 *
 * <p>How much a client owes is its outstanding total: the sum of amount due across all the client's
 * invoices (each invoice's discounted amount less every payment against it), the same per-client
 * derivation {@link ClientsGet} makes for the clients-page "outstanding" sort and
 * {@link ClientStatementGet} makes for a single client's statement. A client that owes nothing is
 * left off the list, and ties fall back to the oldest client (id) first so the order is stable.
 */
public class DashboardTopClientsGet {

    /** How many clients the home screen shows. */
    private static final long TOP_N = 5;

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<List<TopClientView>> response) {
        Map<Long, BigDecimal> owed = outstandingByClient(projects, invoices, payments);
        Map<Long, String> nameById = clients.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        // Each client's figure is shown in their own currency — they are never added together, so the
        // amounts stay comparable for ranking but each renders in its client's currency.
        Map<Long, String> currencyById = Currencies.byClient(clients);

        List<TopClientView> top = owed.entrySet().stream()
                // Only clients that actually owe something rank — a zero (or fully paid) balance is
                // not a "top client by how much they owe".
                .filter(e -> e.getValue().signum() > 0)
                .sorted(Comparator
                        .comparing(Map.Entry<Long, BigDecimal>::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_N)
                .map(e -> new TopClientView(e.getKey(), nameById.get(e.getKey()), e.getValue(),
                        currencyById.getOrDefault(e.getKey(), Currencies.DEFAULT)))
                .collect(Collectors.toList());

        response.send(top);
    }

    /**
     * How much each client still owes, keyed by client id: the sum of amount due across all the
     * client's invoices (each invoice's discounted amount less every payment against it), mirroring
     * {@link ClientsGet}'s outstanding derivation. A client with no invoices is simply absent from
     * the map (treated as owing zero).
     */
    private Map<Long, BigDecimal> outstandingByClient(ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments) {
        // An invoice belongs to a project, and a project belongs to a client — so a project's id maps
        // its invoices back to the owning client.
        Map<Long, Long> clientByProject = projects.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Project::getId, Project::getClientId));

        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        for (Payment payment : payments.findAllByOrderByIdAsc()) {
            paidByInvoice.merge(payment.getInvoiceId(), payment.getAmount(), BigDecimal::add);
        }

        Map<Long, BigDecimal> owed = new HashMap<>();
        for (Invoice invoice : invoices.findAllByOrderByIdAsc()) {
            Long clientId = clientByProject.get(invoice.getProjectId());
            if (clientId == null) {
                continue;
            }
            BigDecimal paid = paidByInvoice.getOrDefault(invoice.getId(), BigDecimal.ZERO);
            BigDecimal due = invoice.getDiscountedAmount().subtract(paid);
            owed.merge(clientId, due, BigDecimal::add);
        }
        return owed;
    }
}
