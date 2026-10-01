package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/one?invoiceId=&lt;id&gt; — one invoice, with its status WORKED OUT from its
 * payments (still owing / part paid / paid) rather than a figure flipped by hand. The invoice detail
 * page reads this to show the derived status; the invoice id arrives as a query parameter. Wired by
 * officefloor/rest/api/invoices/one.GET.yml.
 */
public class GetInvoice {

    public void service(@RequestParam("invoiceId") String invoiceId, InvoiceRepository invoices,
            InvoicePaymentRepository payments, ProjectRepository projects, ClientRepository clients,
            ObjectResponse<InvoiceView> response) {
        Long id = Long.valueOf(invoiceId);
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        BigDecimal paidSum = payments.findByInvoiceIdOrderByIdAsc(id).stream()
                .map(InvoicePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // The invoice is shown in its client's currency (through its project).
        String currency = projects.findById(invoice.getProjectId())
                .flatMap(project -> clients.findById(project.getClientId()))
                .map(Client::getCurrency)
                .orElse("USD");
        response.send(InvoiceView.ofDerived(invoice, paidSum, currency));
    }
}
