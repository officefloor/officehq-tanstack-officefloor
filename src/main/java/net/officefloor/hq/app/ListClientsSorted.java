package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/clients/sorted?sort=&lt;key&gt; — the clients list in a chosen order, for the sort control
 * on the clients page. Archived (tucked-away) clients are excluded, exactly as on the plain list
 * {@link ListClients}. The {@code sort} key chooses the order:
 * <ul>
 *   <li>{@code outstanding} — most-owed-first. Each client's outstanding balance is DERIVED on the
 *       server: the sum, across every invoice of every project the client owns, of the invoice total
 *       minus what has been paid against it, computed in {@link BigDecimal} so there is no float
 *       drift — the same derivation the client statement ({@link GetClientStatement}) uses.</li>
 *   <li>{@code name} — case-insensitive name order.</li>
 *   <li>anything else (and the default) — id order.</li>
 * </ul>
 * Ties break by id for a stable order. This is a NEW endpoint rather than a change to the shared
 * {@link ListClients}, so the plain {@code /api/clients} that the projects picker and global search
 * read stays untouched. Wired by officefloor/rest/api/clients/sorted.GET.yml.
 */
public class ListClientsSorted {

    public void service(@RequestParam("sort") String sort,
            ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, InvoicePaymentRepository payments,
            ObjectResponse<List<ClientView>> response) {
        Comparator<Client> order;
        if ("outstanding".equals(sort)) {
            // Most owed first; equal balances (including everyone at zero) fall back to id order.
            order = Comparator
                    .comparing((Client c) -> outstanding(c, projects, invoices, payments))
                    .reversed()
                    .thenComparing(Client::getId);
        } else if ("name".equals(sort)) {
            order = Comparator
                    .comparing((Client c) -> c.getName().toLowerCase())
                    .thenComparing(Client::getId);
        } else {
            order = Comparator.comparing(Client::getId);
        }
        List<ClientView> view = clients.findAll().stream()
                .filter(client -> !client.isArchived())
                .sorted(order)
                .map(ClientView::of)
                .toList();
        response.send(view);
    }

    /**
     * What one client still owes: the sum, over every invoice of every project the client owns, of
     * the invoice total less the payments recorded against it. The same money-scale BigDecimal
     * derivation the client statement applies, so the two stay consistent.
     */
    private static BigDecimal outstanding(Client client, ProjectRepository projects,
            InvoiceRepository invoices, InvoicePaymentRepository payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (Project project : projects.findByClientIdOrderByIdAsc(client.getId())) {
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                        .map(InvoicePayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                total = total.add(invoice.getTotal().subtract(paid));
            }
        }
        return total;
    }
}
