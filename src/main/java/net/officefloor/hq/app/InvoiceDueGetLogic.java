package net.officefloor.hq.app;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/due?invoiceId=<id>} — how much is still left to pay on one invoice: its
 * amount (the worked-out sum of its line items, Flyway V13/V14) minus everything paid against it (the
 * sum of its payments, Flyway V19). The figure is derived on the server so the money math lives in
 * one place, and an invoice with no payments yet is simply its full amount. Wired by
 * {@code officefloor/rest/api/invoices/due.GET.yml}.
 */
public class InvoiceDueGetLogic {

    public void service(@RequestParam("invoiceId") Long invoiceId, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<InvoiceDueView> response) {
        BigDecimal amount = invoices.findById(invoiceId).map(Invoice::getAmount)
                .orElse(BigDecimal.ZERO);
        BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoiceId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.send(new InvoiceDueView(invoiceId, amount, paid, amount.subtract(paid)));
    }

    /** What is left to pay on one invoice: its amount, what has been paid, and the difference. */
    public static class InvoiceDueView {
        private final long invoiceId;
        private final BigDecimal amount;
        private final BigDecimal paid;
        private final BigDecimal due;

        public InvoiceDueView(long invoiceId, BigDecimal amount, BigDecimal paid, BigDecimal due) {
            this.invoiceId = invoiceId;
            this.amount = amount;
            this.paid = paid;
            this.due = due;
        }

        public long getInvoiceId() {
            return invoiceId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getPaid() {
            return paid;
        }

        public BigDecimal getDue() {
            return due;
        }
    }
}
