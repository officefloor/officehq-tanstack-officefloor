package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard/top-clients — the home dashboard's top five clients, ranked by how much they
 * still owe (most owed first). Each client's outstanding figure is the sum of the dues across all of
 * their invoices, computed exactly the way the client statement and {@link ClientsOutstandingGet} do
 * it ({@link ProjectInvoice#getDue}), so the panel agrees with the rest of the app. Ties fall back to
 * the oldest client id so the order is always stable. Wired by
 * {@code officefloor/rest/api/dashboard/top-clients.GET.yml}.
 */
public class DashboardTopClientsGet {

    private static final int LIMIT = 5;

    public void service(ClientRepository clients, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<List<TopClient>> response) {
        List<TopClient> owed = new ArrayList<>();
        for (Client client : clients.findAllByArchivedFalseOrderByIdAsc()) {
            BigDecimal total = BigDecimal.ZERO;
            for (Invoice invoice : invoices.findByClientId(client.getId())) {
                ProjectInvoice row =
                        new ProjectInvoice(invoice, payments.sumByInvoiceId(invoice.getId()));
                total = total.add(row.getDue());
            }
            owed.add(new TopClient(client.getId(), client.getName(), total));
        }
        owed.sort(Comparator.comparing(TopClient::getOutstanding).reversed()
                .thenComparing(TopClient::getClientId));
        response.send(owed.subList(0, Math.min(LIMIT, owed.size())));
    }
}
