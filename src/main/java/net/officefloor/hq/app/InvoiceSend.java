package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/invoices/send} — send a draft invoice, moving it from DRAFT to SENT, and return
 * the updated row. Wired by {@code officefloor/rest/api/invoices/send.POST.yml}. The invoice must
 * exist and still be a DRAFT; sending it is recorded through {@link Audit} so the send can be
 * checked back later (the UI can't show it).
 */
public class InvoiceSend {

    public void service(@RequestBody SendInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        Invoice invoice = id == null ? null : invoices.findById(id).orElse(null);
        if (invoice == null) {
            throw new IllegalArgumentException("no such invoice");
        }
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("only a draft invoice can be sent");
        }
        invoice.setStatus("SENT");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_SENT id=" + saved.getId() + " amount="
                + saved.getAmount().toPlainString());
        response.send(saved);
    }
}
