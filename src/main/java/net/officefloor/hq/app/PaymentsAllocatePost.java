package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/payments/allocate} — record one lump payment a client has made, split across
 * several of their invoices, and return the created rows. Wired by
 * {@code officefloor/rest/api/payments/allocate.POST.yml}.
 *
 * <p>The whole {@code amount} must be positive, the {@code date} supplied, and every allocation must
 * name an existing invoice with a positive share; the shares must add up to the lump {@code amount}
 * (so nothing of the payment is left unaccounted for). Each allocation becomes one {@link Payment}
 * row against its invoice (so each invoice's balance reflects only its share — the existing amount-due
 * derivation sums the payment rows per invoice), and all of them carry the same {@code batchRef} so
 * the rows that made up this one payment can be traced back together. Each row is audited through
 * {@link Audit}, the same {@code PAYMENT_RECORDED} record a single-invoice payment writes, so the
 * side effect is verifiable where the UI cannot show it.
 */
public class PaymentsAllocatePost {

    public void service(@RequestBody AllocatedPayment body, PaymentRepository payments,
            InvoiceRepository invoices, Audit audit, ObjectResponse<List<Payment>> response) {
        BigDecimal amount = body.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("a payment requires a positive amount");
        }
        String date = body.getDate() == null ? "" : body.getDate().trim();
        if (date.isEmpty()) {
            throw new IllegalArgumentException("a payment requires a date");
        }
        List<AllocatedPayment.Allocation> allocations = body.getAllocations();
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("a split payment requires at least one allocation");
        }

        // Validate every allocation up front and total the shares, so nothing is written unless the
        // whole split is sound (an existing invoice and a positive share each) and the shares add up
        // to the lump amount.
        BigDecimal allocated = BigDecimal.ZERO;
        for (AllocatedPayment.Allocation allocation : allocations) {
            Long invoiceId = allocation.getInvoiceId();
            if (invoiceId == null || invoices.findById(invoiceId).isEmpty()) {
                throw new IllegalArgumentException("an allocation requires an existing invoice");
            }
            BigDecimal share = allocation.getAmount();
            if (share == null || share.signum() <= 0) {
                throw new IllegalArgumentException("an allocation requires a positive amount");
            }
            allocated = allocated.add(share);
        }
        if (allocated.compareTo(amount) != 0) {
            throw new IllegalArgumentException("allocations must add up to the payment amount");
        }

        // One reference shared across every row of this one lump payment, so the allocations can be
        // traced back together (payment.batch_ref).
        String batchRef = UUID.randomUUID().toString();
        List<Payment> saved = new ArrayList<>();
        for (AllocatedPayment.Allocation allocation : allocations) {
            Payment payment = new Payment();
            payment.setInvoiceId(allocation.getInvoiceId());
            payment.setAmount(allocation.getAmount());
            payment.setDate(date);
            payment.setBatchRef(batchRef);
            Payment row = payments.save(payment);
            audit.record("PAYMENT_RECORDED id=" + row.getId() + " amount="
                    + row.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            saved.add(row);
        }

        response.send(saved);
    }
}
