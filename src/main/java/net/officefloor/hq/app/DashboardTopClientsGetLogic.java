package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/dashboard/top-clients} — the home dashboard's "top clients" tile: the five clients
 * who owe the most, ranked by what they still owe. For each client it sums the due across ALL of that
 * client's invoices (invoice -&gt; project -&gt; client), each invoice's due being its net total after
 * any discount/tax (the one-place {@link InvoiceMoney} rule) minus what has been paid against it — the
 * same money math as {@link ClientsOutstandingGetLogic}, worked out on the server so every surface
 * agrees. Only clients that owe something appear; the list is ordered by outstanding descending (ties
 * broken by client id for a stable order) and capped at five. Wired by
 * {@code officefloor/rest/api/dashboard/top-clients.GET.yml}.
 */
public class DashboardTopClientsGetLogic {

    private static final int TOP_N = 5;

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<List<TopClientView>> response) {
        // invoice -> project -> client: map each project to the client it belongs to.
        Map<Long, Long> projectToClient = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getClientId));
        List<TopClientView> rows = clients.findAll().stream()
                .map(client -> {
                    BigDecimal outstanding = invoices.findAll().stream()
                            .filter(i -> client.getId().equals(projectToClient.get(i.getProjectId())))
                            .map(i -> {
                                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(i.getId())
                                        .stream().map(Payment::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                BigDecimal netTotal = InvoiceMoney.netTotal(i.getAmount(),
                                        i.getDiscountPct(), i.getTaxPct());
                                return netTotal.subtract(paid);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopClientView(client.getId(), client.getName(), outstanding);
                })
                // Only clients that actually owe something are "top clients".
                .filter(row -> row.getOutstanding().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TopClientView::getOutstanding).reversed()
                        .thenComparing(TopClientView::getClientId))
                .limit(TOP_N)
                .collect(Collectors.toList());
        response.send(rows);
    }

    /** One client in the ranking: who they are and how much they still owe. */
    public static class TopClientView {
        private final long clientId;
        private final String name;
        private final BigDecimal outstanding;

        public TopClientView(long clientId, String name, BigDecimal outstanding) {
            this.clientId = clientId;
            this.name = name;
            this.outstanding = outstanding;
        }

        public long getClientId() {
            return clientId;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getOutstanding() {
            return outstanding;
        }
    }
}
