package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/payments/split} — record one lump-sum payment a client made and split it across
 * several of their open invoices, from a {amount, date, allocations:[{invoiceId, amount}]} body.
 * Each allocation is saved as its own payment row against its invoice, so every invoice's balance and
 * status derive the same one-place way from the payments recorded against it (see
 * {@link InvoiceDueGetLogic} and {@link InvoiceStatus}); all the rows carry a shared batch reference
 * (Flyway V30) so the split is recorded as one payment. Wired by
 * {@code officefloor/rest/api/payments/split.POST.yml}.
 *
 * <p>Validated here and rejected with 400: a date is required and must be ISO; there must be at least
 * one allocation; each allocation needs an existing invoice and a positive amount; and the
 * allocations must add up to the lump amount, so no money is lost or invented when it is split.
 * Appends one {@code PAYMENT_RECORDED} audit record per allocation, matching the single-invoice path
 * ({@link PaymentsPostLogic}).
 */
public class PaymentsSplitLogic {

    public void service(@RequestBody SplitPayment body, PaymentRepository payments,
            InvoiceRepository invoices, Audit audit, ObjectResponse<List<Payment>> response) {
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

        List<Allocation> allocations = body.getAllocations();
        if (allocations == null || allocations.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "At least one allocation is required");
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (Allocation allocation : allocations) {
            Long invoiceId = allocation.getInvoiceId();
            if (invoiceId == null || !invoices.existsById(invoiceId)) {
                throw new HttpException(HttpStatus.BAD_REQUEST, "A valid invoice is required");
            }
            BigDecimal amount = allocation.getAmount();
            if (amount == null || amount.signum() <= 0) {
                throw new HttpException(HttpStatus.BAD_REQUEST, "A positive amount is required");
            }
            allocated = allocated.add(amount);
        }

        BigDecimal lump = body.getAmount();
        if (lump == null || lump.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A positive amount is required");
        }
        // The split must account for the whole lump exactly — otherwise an invoice's balance would not
        // come out right. Compare by value (ignoring scale) so 150 and 150.00 agree.
        if (allocated.compareTo(lump) != 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "The allocations must add up to the payment amount");
        }

        String batchRef = UUID.randomUUID().toString();
        List<Payment> saved = new ArrayList<>();
        for (Allocation allocation : allocations) {
            Payment payment = payments.save(
                    new Payment(allocation.getInvoiceId(), allocation.getAmount(), date, batchRef));
            saved.add(payment);
            String amountText =
                    allocation.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            audit.record(
                    "PAYMENT_RECORDED id=" + allocation.getInvoiceId() + " amount=" + amountText);
        }
        response.send(saved);
    }

    /** Request body: a lump payment and how it is split across the client's open invoices. */
    public static class SplitPayment {
        private BigDecimal amount;
        private String date;
        private List<Allocation> allocations;

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

        public List<Allocation> getAllocations() {
            return allocations;
        }

        public void setAllocations(List<Allocation> allocations) {
            this.allocations = allocations;
        }
    }

    /** One share of the lump payment: how much goes against which invoice. */
    public static class Allocation {
        private Long invoiceId;
        private BigDecimal amount;

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
    }
}
