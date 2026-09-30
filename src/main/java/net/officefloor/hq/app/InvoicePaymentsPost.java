package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/{id}/payments — record a payment against an invoice from {amount, date} and
 * return the saved row (with its id). Wired by
 * {@code officefloor/rest/api/invoices/{id}/payments.POST.yml}. A payment needs an amount above zero
 * and a date; anything missing/invalid is rejected with 400 and an unknown invoice with 404.
 *
 * Recording a payment replaces flipping the invoice to paid by hand (the invoice's status is derived
 * from its payments — see {@link ProjectInvoice#deriveStatus}), so it is an audited side-effect: one
 * {@code PAYMENT_RECORDED id=<id> amount=<amount>} record is appended per payment (CLAUDE.md —
 * audited behaviour goes through {@link Audit}).
 */
public class InvoicePaymentsPost {

    public void service(@HttpPathParameter("id") String id, @RequestBody NewPayment body,
            InvoiceRepository invoices, PaymentRepository payments, Audit audit,
            ObjectResponse<Payment> response) {
        Long invoiceId = Long.valueOf(id);
        invoices.findById(invoiceId).orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        BigDecimal amount = body.getAmount();
        String date = body.getDate();
        if (amount == null || amount.signum() <= 0 || date == null || date.isBlank()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setDate(date.trim());
        Payment saved = payments.save(payment);
        audit.record("PAYMENT_RECORDED id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(saved);
    }
}
