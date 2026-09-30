package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/invoices/{id}/send — move a DRAFT invoice to SENT and return the updated row. Wired by
 * {@code officefloor/rest/api/invoices/{id}/send.POST.yml}. Sending is an audited side-effect: one
 * {@code INVOICE_SENT id=<id> amount=<amount>} record is appended per send so it can be checked
 * back later (CLAUDE.md — audited behaviour goes through {@link Audit}). Only a DRAFT invoice can be
 * sent; an unknown id is rejected with 404 and a non-draft invoice with 400.
 */
public class InvoiceSend {

    public void service(@HttpPathParameter("id") String id, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Invoice invoice = invoices.findById(Long.valueOf(id))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        invoice.setStatus("SENT");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_SENT id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(saved);
    }
}
