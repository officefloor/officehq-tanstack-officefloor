package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/invoices/pay} — mark an invoice paid and return the updated row. Wired by
 * {@code officefloor/rest/api/invoices/pay.POST.yml}. The invoice must exist and have been SENT
 * (payment is only allowed once it has been sent); flipping it to PAID is recorded through
 * {@link Audit} so the payment can be checked back later (the UI can't show it).
 */
public class InvoicePay {

    public void service(@RequestBody PayInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        Invoice invoice = id == null ? null : invoices.findById(id).orElse(null);
        if (invoice == null) {
            throw new IllegalArgumentException("no such invoice");
        }
        if (!"SENT".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("an invoice can only be paid once it has been sent");
        }
        invoice.setStatus("PAID");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_PAID id=" + saved.getId() + " amount="
                + saved.getAmount().toPlainString());
        response.send(saved);
    }
}
