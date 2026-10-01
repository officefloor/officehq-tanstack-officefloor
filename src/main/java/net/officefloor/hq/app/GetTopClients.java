package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/dashboard/top-clients — the home screen's leaderboard: the clients that owe the most,
 * ranked by how much they owe, capped at the top five. What a client owes is the sum of what is
 * still DUE across every invoice their projects hold (each invoice's total minus what has been paid
 * against it) — the same derivation {@link GetClientStatement} totals, so the ranking agrees with
 * each client's statement. Clients that owe nothing are left off (there is nothing to rank), and
 * archived clients drop off here just as they drop off the client list. Ties are broken by client
 * id so the order is stable. A cross-entity aggregate read, so it injects the repositories it needs.
 * Wired by officefloor/rest/api/dashboard/top-clients.GET.yml.
 */
public class GetTopClients {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, InvoicePaymentRepository payments,
            ObjectResponse<List<DashboardTopClientView>> response) {
        List<DashboardTopClientView> ranked = clients.findAll().stream()
                .filter(client -> !client.isArchived())
                .map(client -> new DashboardTopClientView(client.getId(), client.getName(),
                        owedBy(client, projects, invoices, payments), client.getCurrency()))
                .filter(row -> row.amount().signum() > 0)
                .sorted(Comparator.comparing(DashboardTopClientView::amount).reversed()
                        .thenComparing(DashboardTopClientView::clientId))
                .limit(5)
                .toList();
        response.send(ranked);
    }

    /** What one client still owes: the due balance summed across all its projects' invoices. */
    private static BigDecimal owedBy(Client client, ProjectRepository projects,
            InvoiceRepository invoices, InvoicePaymentRepository payments) {
        BigDecimal owed = BigDecimal.ZERO;
        for (Project project : projects.findByClientIdOrderByIdAsc(client.getId())) {
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                        .map(InvoicePayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                owed = owed.add(invoice.getTotal().subtract(paid));
            }
        }
        return owed;
    }
}
