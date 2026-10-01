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
        invoice.markPaid();
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_PAID id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(InvoiceView.of(saved));
    }
}
