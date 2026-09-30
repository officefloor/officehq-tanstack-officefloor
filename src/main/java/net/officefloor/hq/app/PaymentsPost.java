package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/payments} — record a payment (amount and date) a client has made against an
 * invoice and return the created row. Wired by {@code officefloor/rest/api/payments.POST.yml}. The
 * invoice must exist, the amount must be positive, and the date must be supplied. The payment is
 * audited through {@link Audit} so the side effect is verifiable where the UI cannot show it.
 */
public class PaymentsPost {

    public void service(@RequestBody NewPayment body, PaymentRepository payments,
            InvoiceRepository invoices, Audit audit, ObjectResponse<Payment> response) {
        Long invoiceId = body.getInvoiceId();
        Invoice invoice = invoiceId == null ? null : invoices.findById(invoiceId).orElse(null);
        if (invoice == null) {
            throw new IllegalArgumentException("a payment requires an existing invoice");
        }
        BigDecimal amount = body.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("a payment requires a positive amount");
        }
        String date = body.getDate() == null ? "" : body.getDate().trim();
        if (date.isEmpty()) {
            throw new IllegalArgumentException("a payment requires a date");
        }
        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setDate(date);
        Payment saved = payments.save(payment);

        audit.record("PAYMENT_RECORDED id=" + saved.getId() + " invoice=" + invoiceId
                + " amount=" + saved.getAmount().toPlainString() + " date=" + saved.getDate());
        response.send(saved);
    }
}
