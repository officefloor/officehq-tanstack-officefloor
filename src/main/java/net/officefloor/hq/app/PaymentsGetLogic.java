package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/payments?invoiceId=<id>} — list the payments recorded against one invoice, oldest
 * first, so an invoice's detail page shows only its own payments (amount and date). Wired by
 * {@code officefloor/rest/api/payments.GET.yml}.
 */
public class PaymentsGetLogic {

    public void service(@RequestParam("invoiceId") Long invoiceId, PaymentRepository payments,
            ObjectResponse<List<Payment>> response) {
        response.send(payments.findByInvoiceIdOrderByIdAsc(invoiceId));
    }
}
