package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{id}/status — one invoice with its status worked out from the payments recorded
 * against it (PAID / PARTIAL / the stored SENT-or-DRAFT stage), plus its amount and the paid/due
 * split. Wired by {@code officefloor/rest/api/invoices/{id}/status.GET.yml}. The invoice's detail
 * page reads this so its status reflects the payments without the invoice being flipped by hand; the
 * derivation itself lives in {@link ProjectInvoice#deriveStatus}. An unknown invoice id is 404.
 */
public class InvoiceStatusGet {

    public void service(@HttpPathParameter("id") String id, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<ProjectInvoice> response) {
        Long invoiceId = Long.valueOf(id);
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        response.send(new ProjectInvoice(invoice, payments.sumByInvoiceId(invoiceId)));
    }
}
