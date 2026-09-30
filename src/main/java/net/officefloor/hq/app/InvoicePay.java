package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/invoices/{id}/pay — mark an invoice paid and return the updated row. Wired by
 * {@code officefloor/rest/api/invoices/{id}/pay.POST.yml}. Flipping the status is an audited
 * side-effect: one {@code INVOICE_PAID id=<id> amount=<amount>} record is appended per payment so
 * it can be checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}). An
 * unknown invoice id is rejected with 404.
 */
public class InvoicePay {

    public void service(@HttpPathParameter("id") String id, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Invoice invoice = invoices.findById(Long.valueOf(id))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        invoice.setStatus("PAID");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_PAID id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(saved);
    }
}
