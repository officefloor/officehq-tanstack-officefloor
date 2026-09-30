package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/invoices/{id}/void — cancel a SENT invoice raised by mistake, moving it to VOID so it
 * no longer counts toward what is owed (only SENT invoices feed {@link InvoiceRepository#sumOutstanding}).
 * Wired by {@code officefloor/rest/api/invoices/{id}/void.POST.yml}. Voiding is an audited
 * side-effect: one {@code INVOICE_VOIDED id=<id> amount=<amount>} record is appended per void so it
 * can be checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}). Only a SENT
 * invoice can be voided; an unknown id is rejected with 404 and any other stage with 400.
 */
public class InvoiceVoid {

    public void service(@HttpPathParameter("id") String id, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Invoice invoice = invoices.findById(Long.valueOf(id))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        if (!"SENT".equals(invoice.getStatus())) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        invoice.setStatus("VOID");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_VOIDED id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(saved);
    }
}
