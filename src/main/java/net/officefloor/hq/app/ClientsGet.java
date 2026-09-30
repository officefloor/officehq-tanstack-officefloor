package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients} — list non-archived clients, oldest first. Wired by
 * {@code officefloor/rest/api/clients.GET.yml}. Archived clients are kept but tucked away, so they
 * drop off both this list and the search.
 *
 * <p>An optional {@code q} query parameter narrows the list to clients whose name contains it
 * (case-insensitive) — this backs the name search box on the clients page. When {@code q} is
 * absent or blank, every non-archived client is returned.
 *
 * <p>An optional {@code sort} query parameter reorders the narrowed list, backing the clients-page
 * sort control: {@code name} orders alphabetically (case-insensitive), {@code outstanding} orders
 * by how much each client still owes, most owed first. Absent or blank keeps the stable
 * oldest-first order, so sorting only reorders the list, never changes which clients it contains.
 */
public class ClientsGet {

    public void service(@HttpQueryParameter("q") String q, @HttpQueryParameter("sort") String sort,
            ClientRepository clients, ProjectRepository projects, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<List<Client>> response) {
        String term = q == null ? "" : q.trim();
        List<Client> list = term.isEmpty()
                ? clients.findByArchivedFalseOrderByIdAsc()
                : clients.findByArchivedFalseAndNameContainingIgnoreCaseOrderByIdAsc(term);

        String order = sort == null ? "" : sort.trim();
        if ("name".equals(order)) {
            list = list.stream()
                    .sorted(Comparator.comparing(c -> c.getName() == null ? "" : c.getName(),
                            String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } else if ("outstanding".equals(order)) {
            // How much each client owes, so most-owed comes first; oldest-first (id) breaks ties, so
            // two clients owing the same still have a stable order.
            Map<Long, BigDecimal> owed = outstandingByClient(projects, invoices, payments);
            list = list.stream()
                    .sorted(Comparator
                            .comparing((Client c) -> owed.getOrDefault(c.getId(), BigDecimal.ZERO))
                            .reversed()
                            .thenComparing(Client::getId))
                    .collect(Collectors.toList());
        }

        response.send(list);
    }

    /**
     * How much each client still owes, keyed by client id: the sum of amount due across all the
     * client's invoices (each invoice's discounted amount less every payment against it), the same
     * derivation {@link ClientStatementGet} makes for a single client's outstanding total. A client
     * with no invoices is simply absent from the map (treated as owing zero).
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
