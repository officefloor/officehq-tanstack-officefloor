package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/payments} — record a payment against an invoice from a {invoiceId, amount, date}
 * body and return the saved row (with its generated id). Wired by
 * {@code officefloor/rest/api/payments.POST.yml}. A payment must belong to an existing invoice,
 * carry a positive amount and an ISO date; all are validated here and rejected with 400.
 */
public class PaymentsPostLogic {

    public void service(@RequestBody NewPayment body, PaymentRepository payments,
            InvoiceRepository invoices, ObjectResponse<Payment> response) {
        Long invoiceId = body.getInvoiceId();
        if (invoiceId == null || !invoices.existsById(invoiceId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid invoice is required");
        }
        BigDecimal amount = body.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A positive amount is required");
        }
        String dateText = body.getDate();
        if (dateText == null || dateText.isBlank()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A date is required");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException e) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid date is required");
        }

        Payment saved = payments.save(new Payment(invoiceId, amount, date));
        response.send(saved);
    }

    /** Request body for recording a payment against an invoice. */
    public static class NewPayment {
        private Long invoiceId;
        private BigDecimal amount;
        private String date;

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}
