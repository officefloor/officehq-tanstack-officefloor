package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/pay — mark an invoice paid: flip its status to PAID and return the updated row.
 * Wired by officefloor/rest/api/invoices/pay.POST.yml.
 *
 * Paying is an audited side-effect: alongside the status change we append one record to the audit
 * file through the {@link Audit} service ({@code INVOICE_PAID id=<id> amount=<amount>}) so the user
 * can check back later — the record is the durable trail the UI can't show. We reject a missing or
 * unknown invoice id before writing anything so a bad request neither changes state nor audits.
 *
 * Payment follows the lifecycle: an invoice can only be paid once it has been SENT. We reject a pay
 * on an invoice that hasn't been sent so the DRAFT -> SENT -> PAID order holds server-side too, not
 * just in the UI that hides the control.
 */
public class PayInvoice {

    public void service(@RequestBody PayInvoiceForm form, InvoiceRepository invoices, Audit audit,
            ObjectResponse<InvoiceView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        if (!invoice.isSent()) {
            throw new IllegalArgumentException("An invoice can only be paid once it has been sent");
        }
        invoice.markPaid();
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_PAID id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(InvoiceView.of(saved));
    }
}
