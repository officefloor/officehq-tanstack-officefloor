package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/{clientId}/payments — record ONE lump payment a client made and split it across
 * several of their open invoices. The body carries the total amount, the date, and a per-invoice
 * allocation list; this inserts one {@link Payment} row per allocation, so each invoice's balance
 * then reflects its share (the invoice's due and status are DERIVED from its payments — see
 * {@link ProjectInvoice}). Returns the saved payment rows. Wired by
 * {@code officefloor/rest/api/clients/{clientId}/payments.POST.yml}.
 *
 * The lump needs an amount above zero and a date, and at least one allocation with an amount above
 * zero (allocations left blank are skipped); anything missing/invalid is rejected with 400 and an
 * allocation naming an unknown invoice with 404. As with the per-invoice payment path, each payment
 * is an audited side-effect: one {@code PAYMENT_RECORDED id=<id> amount=<amount>} record is appended
 * per payment created (CLAUDE.md — audited behaviour goes through {@link Audit}).
 */
public class ClientPaymentsPost {

    public void service(@HttpPathParameter("clientId") String clientId,
            @RequestBody NewClientPayment body, InvoiceRepository invoices,
            PaymentRepository payments, Audit audit, ObjectResponse<List<Payment>> response) {
        BigDecimal amount = body.getAmount();
        String date = body.getDate();
        List<PaymentAllocation> allocations = body.getAllocations();
        if (amount == null || amount.signum() <= 0 || date == null || date.isBlank()
                || allocations == null || allocations.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        List<Payment> saved = new ArrayList<>();
        for (PaymentAllocation allocation : allocations) {
            BigDecimal share = allocation.getAmount();
            if (allocation.getInvoiceId() == null || share == null || share.signum() <= 0) {
                continue; // an invoice the client did not put any of this payment toward
            }
            Long invoiceId = allocation.getInvoiceId();
            invoices.findById(invoiceId).orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
            Payment payment = new Payment();
            payment.setInvoiceId(invoiceId);
            payment.setAmount(share);
            payment.setDate(date.trim());
            Payment row = payments.save(payment);
            audit.record("PAYMENT_RECORDED id=" + row.getId() + " amount="
                    + row.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            saved.add(row);
        }
        if (saved.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        response.send(saved);
    }
}
