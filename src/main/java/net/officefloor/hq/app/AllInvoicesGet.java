package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices — every invoice across every project, each with the name of the project it
 * belongs to and its lifecycle stage, oldest id first. One place listing all invoices. Wired by
 * {@code officefloor/rest/api/invoices.GET.yml}.
 */
public class AllInvoicesGet {

    public void service(InvoiceRepository invoices, ObjectResponse<List<InvoiceView>> response) {
        response.send(invoices.findAllViews());
    }
}
