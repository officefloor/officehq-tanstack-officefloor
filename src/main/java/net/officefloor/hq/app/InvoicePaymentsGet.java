package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{id}/payments — every payment recorded against one invoice, oldest id first.
 * Wired by {@code officefloor/rest/api/invoices/{id}/payments.GET.yml}. The front-end lists each
 * payment's amount and date on the invoice.
 */
public class InvoicePaymentsGet {

    public void service(@HttpPathParameter("id") String id, PaymentRepository payments,
            ObjectResponse<List<Payment>> response) {
        response.send(payments.findByInvoiceIdOrderByIdAsc(Long.valueOf(id)));
    }
}
