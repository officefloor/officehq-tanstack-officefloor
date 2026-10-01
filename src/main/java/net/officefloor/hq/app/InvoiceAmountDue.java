package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/due?invoiceId=&lt;id&gt; — how much is still left to pay on one invoice after any
 * payments. The amount due is DERIVED: the invoice's amount minus the sum of its payments, computed
 * in {@link BigDecimal} so there is no float drift. Scoped to an invoice (each row asks for its own
 * balance), so the invoice id arrives as a query parameter. Wired by
 * officefloor/rest/api/invoices/due.GET.yml.
 */
public class InvoiceAmountDue {

    public void service(@RequestParam("invoiceId") String invoiceId,
            InvoiceRepository invoices, InvoicePaymentRepository payments,
            ObjectResponse<InvoiceDueView> response) {
        Long id = Long.valueOf(invoiceId);
        BigDecimal amount = invoices.findById(id).map(Invoice::getTotal).orElse(BigDecimal.ZERO);
        BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(id).stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.send(new InvoiceDueView(id, amount.subtract(paid)));
    }
}
