package net.officefloor.hq.app;

import java.math.RoundingMode;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/invoices/pay} — mark an invoice paid from an {id} body and return the updated row.
 * Wired by {@code officefloor/rest/api/invoices/pay.POST.yml}. Flips the invoice's status to PAID and
 * appends one {@code INVOICE_PAID} audit record (the kept record the request asks for), so the
 * payment can be checked back later through the audit file even though the UI only shows the status.
 */
public class InvoicesPayLogic {

    public void service(@RequestBody PayInvoice body, InvoiceRepository invoices, Audit audit,
            ObjectResponse<Invoice> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such invoice"));
        // Payment is only allowed once the invoice has been sent: a DRAFT must be sent first.
        if (!"SENT".equals(invoice.getStatus())) {
            throw new HttpException(HttpStatus.CONFLICT, "An invoice must be sent before it can be paid");
        }
        invoice.setStatus("PAID");
        Invoice saved = invoices.save(invoice);
        String amount = saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        audit.record("INVOICE_PAID id=" + saved.getId() + " amount=" + amount);
        response.send(saved);
    }

    /** Request body for marking an invoice paid. */
    public static class PayInvoice {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
