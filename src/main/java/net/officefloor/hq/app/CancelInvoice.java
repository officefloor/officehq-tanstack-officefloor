package net.officefloor.hq.app;

import java.math.RoundingMode;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices/cancel — cancel an invoice sent by mistake: flip its status to VOID and return
 * the updated row. Wired by officefloor/rest/api/invoices/cancel.POST.yml.
 *
 * A VOID invoice is terminal and no longer counts towards what is owed (the dashboard's outstanding
 * total and overdue count only ever sum SENT invoices, so a voided one drops out of both). Cancelling
 * is an audited side-effect: alongside the status change we append one record to the audit file
 * through the {@link Audit} service ({@code INVOICE_VOIDED id=<id> amount=<amount>}) so there is a
 * durable record every time an invoice is voided — the trail the UI can't show. We reject a missing
 * or unknown invoice id before writing anything so a bad request neither changes state nor audits.
 */
public class CancelInvoice {

    public void service(@RequestBody CancelInvoiceForm form, InvoiceRepository invoices, Audit audit,
            ObjectResponse<InvoiceView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("An invoice id is required");
        }
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid invoice is required"));
        invoice.markVoid();
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_VOIDED id=" + saved.getId() + " amount="
                + saved.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.send(InvoiceView.of(saved));
    }
}
