package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices} — list every invoice, oldest first, each carrying the id of the project
 * it belongs to so the project-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/invoices.GET.yml}.
 *
 * <p>An optional {@code sort=due} query parameter orders the list by due date (earliest first) —
 * this backs the sort-by-due-date control on the project invoices list. Any other value (or none)
 * keeps the stable oldest-first order.
 *
 * <p>Each invoice also carries its {@code amountDue}: the amount still owed after payments (its
 * amount minus every payment recorded against it). This is derived here from the payments — not
 * stored — so the project invoices view can show how much is left to pay on each invoice.
 */
public class InvoicesGet {

    public void service(@HttpQueryParameter("sort") String sort, InvoiceRepository repository,
            PaymentRepository payments, ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<Invoice>> response) {
        List<Invoice> invoices = "due".equals(sort == null ? null : sort.trim())
                ? repository.findAllByOrderByDueDateAscIdAsc()
                : repository.findAllByOrderByIdAsc();

        // Sum the payments per invoice once, then set each invoice's derived amount due (amount less
        // what has been paid) so the row shows what is still owed.
        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        for (Payment payment : payments.findAllByOrderByIdAsc()) {
            paidByInvoice.merge(payment.getInvoiceId(), payment.getAmount(), BigDecimal::add);
        }
        // Each invoice is shown in its client's currency: an invoice belongs to a project, and a
        // project to a client, so map the invoice's project back to that client's currency.
        Map<Long, String> currencyByProject = Currencies.byProject(projects, clients);
        for (Invoice invoice : invoices) {
            BigDecimal paid = paidByInvoice.getOrDefault(invoice.getId(), BigDecimal.ZERO);
            invoice.setAmountDue(invoice.getAmount().subtract(paid));
            invoice.setCurrency(currencyByProject.get(invoice.getProjectId()));
        }

        response.send(invoices);
    }
}
