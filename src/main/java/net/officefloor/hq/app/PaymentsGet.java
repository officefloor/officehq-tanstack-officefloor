package net.officefloor.hq.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/payments} — list every payment, oldest first, each carrying the id of the invoice
 * it was made against so the invoice-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/payments.GET.yml}. Each payment also carries the currency it is shown in
 * — the billing currency of the client its invoice's project belongs to — so the invoice-detail
 * payments read in the same currency as the invoice.
 */
public class PaymentsGet {

    public void service(PaymentRepository payments, InvoiceRepository invoices,
            ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<Payment>> response) {
        // A payment knows its invoice, an invoice its project, a project its client — so map each
        // payment's invoice back to that client's currency.
        Map<Long, String> currencyByProject = Currencies.byProject(projects, clients);
        Map<Long, String> currencyByInvoice = new HashMap<>();
        for (Invoice invoice : invoices.findAllByOrderByIdAsc()) {
            currencyByInvoice.put(invoice.getId(),
                    currencyByProject.getOrDefault(invoice.getProjectId(), Currencies.DEFAULT));
        }
        List<Payment> list = payments.findAllByOrderByIdAsc();
        for (Payment payment : list) {
            payment.setCurrency(
                    currencyByInvoice.getOrDefault(payment.getInvoiceId(), Currencies.DEFAULT));
        }
        response.send(list);
    }
}
