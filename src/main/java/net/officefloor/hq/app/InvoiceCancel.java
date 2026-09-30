package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/invoices/cancel} — cancel (void) an invoice that was sent by mistake, moving it
 * from SENT to VOID, and return the updated row. Wired by
 * {@code officefloor/rest/api/invoices/cancel.POST.yml}. The invoice must exist and still be SENT
 * (only a sent invoice can be cancelled — a DRAFT has not gone out and a PAID one is settled). Once
 * VOID it stops counting toward what is owed ({@link InvoiceRepository#sumSentAmount()} only sums
 * SENT). Voiding is recorded through {@link Audit} so the cancellation can be checked back later
 * (the UI can't show it).
 */
public class InvoiceCancel {

    public void service(@RequestBody CancelInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        Invoice invoice = id == null ? null : invoices.findById(id).orElse(null);
        if (invoice == null) {
            throw new IllegalArgumentException("no such invoice");
        }
        if (!"SENT".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("only a sent invoice can be cancelled");
        }
        invoice.setStatus("VOID");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_VOIDED id=" + saved.getId() + " amount="
                + saved.getAmount().toPlainString());
        response.send(saved);
    }
}
