package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/payments — record a payment a client has made against an invoice from the
 * submitted amount and date, returning the saved payment (with its generated id). Wired by
 * officefloor/rest/api/invoices/payments.POST.yml.
 *
 * A payment must name a real invoice, be for a positive amount, and carry a valid date: we reject a
 * missing/unknown invoice id, a missing/non-positive amount, or a missing/unparseable date before
 * persisting so a bad payment can never be saved. Payments are recorded against the invoice but do
 * NOT change its amount (that stays the sum of its line items).
 */
public class CreatePayment {

    public void service(@RequestBody PaymentForm form, InvoiceRepository invoices,
            InvoicePaymentRepository payments, ObjectResponse<PaymentView> response) {
        Long invoiceId = form.getInvoiceId();
        if (invoiceId == null) {
            throw new IllegalArgumentException("An invoice id is required");
        }
        invoices.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        BigDecimal amount = form.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("A payment amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A payment amount must be greater than zero");
        }
        String date = form.getDate();
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("A payment date is required");
        }
        LocalDate paidDate;
        try {
            paidDate = LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A valid payment date is required");
        }
        InvoicePayment saved =
                payments.save(new InvoicePayment(invoiceId, amount, paidDate));
        response.send(PaymentView.of(saved));
    }
}
