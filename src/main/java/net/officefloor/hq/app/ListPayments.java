package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/payments?invoiceId=&lt;id&gt; — the payments made against one invoice, in id
 * order. The invoice detail page lists ITS payments, so the invoice id arrives as a query parameter.
 * Wired by officefloor/rest/api/invoices/payments.GET.yml.
 */
public class ListPayments {

    public void service(@RequestParam("invoiceId") String invoiceId,
            InvoicePaymentRepository payments, ObjectResponse<List<PaymentView>> response) {
        Long id = Long.valueOf(invoiceId);
        List<PaymentView> view = payments.findByInvoiceIdOrderByIdAsc(id).stream()
                .map(PaymentView::of).toList();
        response.send(view);
    }
}
