package net.officefloor.hq.app;

import java.math.RoundingMode;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/invoices/void} — cancel (void) a sent invoice from an {id} body and return the
 * updated row. Wired by {@code officefloor/rest/api/invoices/void.POST.yml}. An invoice sent by
 * mistake is voided: its status flips to VOID so it no longer counts toward what is owed (the
 * dashboard outstanding total only sums SENT invoices), and one {@code INVOICE_VOIDED} audit record
 * is appended (the kept record the request asks for — "note it") so the cancellation can be checked
 * back later through the audit file even though the UI only shows the status. Only a SENT invoice
 * can be voided.
 */
public class InvoicesVoidLogic {

    public void service(@RequestBody VoidInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such invoice"));
        if (!"SENT".equals(invoice.getStatus())) {
            throw new HttpException(HttpStatus.CONFLICT, "Only a sent invoice can be cancelled");
        }
        invoice.setStatus("VOID");
        Invoice saved = invoices.save(invoice);
        String amount = saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        audit.record("INVOICE_VOIDED id=" + saved.getId() + " amount=" + amount);
        response.send(saved);
    }

    /** Request body for cancelling an invoice. */
    public static class VoidInvoice {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
