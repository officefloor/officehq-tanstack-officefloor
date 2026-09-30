package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/payments} — list every payment, oldest first, each carrying the id of the invoice
 * it was made against so the invoice-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/payments.GET.yml}.
 */
public class PaymentsGet {

    public void service(PaymentRepository payments, ObjectResponse<List<Payment>> response) {
        response.send(payments.findAllByOrderByIdAsc());
    }
}
