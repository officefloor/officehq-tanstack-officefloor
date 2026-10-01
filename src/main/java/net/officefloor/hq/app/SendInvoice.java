package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/send — send an invoice: flip its status DRAFT -> SENT and return the updated
 * row. Wired by officefloor/rest/api/invoices/send.POST.yml.
 *
 * Sending is an audited side-effect: alongside the status change we append one record to the audit
 * file through the {@link Audit} service ({@code INVOICE_SENT id=<id> amount=<amount>}) so there is
 * a durable record every time an invoice is sent — the trail the UI can't show. We reject a missing
 * or unknown invoice id before writing anything so a bad request neither changes state nor audits.
 */
public class SendInvoice {

    public void service(@RequestBody SendInvoiceForm form, InvoiceRepository invoices, Audit audit,
            ObjectResponse<InvoiceView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        invoice.markSent();
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_SENT id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(InvoiceView.of(saved));
    }
}
