package net.officefloor.hq.app;

import java.math.RoundingMode;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/invoices/send} — send a DRAFT invoice from an {id} body and return the updated row.
 * Wired by {@code officefloor/rest/api/invoices/send.POST.yml}. Flips the invoice's status to SENT and
 * appends one {@code INVOICE_SENT} audit record (the kept record the request asks for), so the send
 * can be checked back later through the audit file even though the UI only shows the status. Only a
 * DRAFT invoice can be sent; sending is also what unlocks payment (see {@link InvoicesPayLogic}).
 */
public class InvoicesSendLogic {

    public void service(@RequestBody SendInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such invoice"));
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new HttpException(HttpStatus.CONFLICT, "Only a draft invoice can be sent");
        }
        invoice.setStatus("SENT");
        Invoice saved = invoices.save(invoice);
        String amount = saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        audit.record("INVOICE_SENT id=" + saved.getId() + " amount=" + amount);
        response.send(saved);
    }

    /** Request body for sending an invoice. */
    public static class SendInvoice {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
