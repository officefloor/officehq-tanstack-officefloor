package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/clients/payments — record one lump payment a client made and split it across several of
 * their open invoices, returning the saved payments (each with its generated id). Wired by
 * officefloor/rest/api/clients/payments.POST.yml.
 *
 * A split payment must name a real client, carry a valid date, and allocate to that client's own
 * invoices: we reject a missing/unknown client, a missing/unparseable date, an empty allocation
 * list, a non-positive slice, an invoice the client does not own, and a set of slices that does not
 * add up to the stated total — before persisting, so a bad split can never be saved. Each slice
 * lands as its own {@link InvoicePayment} row (the existing per-invoice shape), so each invoice's
 * balance and WORKED-OUT status then reflect its share — exactly as recording a single payment does.
 *
 * It is an audited side-effect: for each slice we append one record through the {@link Audit} service
 * ({@code PAYMENT_RECORDED id=<invoiceId> amount=<amount>}, the same record {@link CreatePayment}
 * emits) so there is a durable trail the UI can't show. We validate the whole split first, so a bad
 * request records nothing.
 */
public class AllocateClientPayment {

    public void service(@RequestBody ClientPaymentForm form, ClientRepository clients,
            ProjectRepository projects, InvoiceRepository invoices,
            InvoicePaymentRepository payments, Audit audit,
            ObjectResponse<List<PaymentView>> response) {
        Long clientId = form.getClientId();
        if (clientId == null) {
            throw new IllegalArgumentException("A client id is required");
        }
        clients.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("A valid client is required"));

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

        List<PaymentAllocation> allocations = form.getAllocations();
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("At least one allocation is required");
        }

        // The invoices this client owns (across all its projects) — a slice may only land on one of
        // these, so a lump payment can never be allocated to another client's invoice.
        Set<Long> clientInvoiceIds = new HashSet<>();
        for (Project project : projects.findByClientIdOrderByIdAsc(clientId)) {
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                clientInvoiceIds.add(invoice.getId());
            }
        }

        BigDecimal allocated = BigDecimal.ZERO;
        for (PaymentAllocation allocation : allocations) {
            Long invoiceId = allocation.getInvoiceId();
            if (invoiceId == null || !clientInvoiceIds.contains(invoiceId)) {
                throw new IllegalArgumentException("Each allocation must name one of the client's invoices");
            }
            BigDecimal amount = allocation.getAmount();
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("Each allocation must be greater than zero");
            }
            allocated = allocated.add(amount);
        }

        // The slices have to add up to the lump sum the client paid, so the split accounts for the
        // whole amount and no money is lost or conjured. Compared at money scale (no float drift).
        BigDecimal total = form.getAmount();
        if (total != null && allocated.setScale(2, RoundingMode.HALF_UP)
                .compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("Allocations must add up to the payment amount");
        }

        // Validated whole — now persist each slice as its own payment and audit it.
        List<PaymentView> saved = new ArrayList<>();
        for (PaymentAllocation allocation : allocations) {
            InvoicePayment payment = payments.save(
                    new InvoicePayment(allocation.getInvoiceId(), allocation.getAmount(), paidDate));
            audit.record("PAYMENT_RECORDED id=" + payment.getInvoiceId() + " amount="
                    + payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            saved.add(PaymentView.of(payment));
        }
        response.send(saved);
    }
}
