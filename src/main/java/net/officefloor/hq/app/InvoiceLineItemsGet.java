package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{id}/lineitems — every line on one invoice, oldest id first. Wired by
 * {@code officefloor/rest/api/invoices/{id}/lineitems.GET.yml}. The front-end sums each line's
 * qty * unitPrice to show the invoice total.
 */
public class InvoiceLineItemsGet {

    public void service(@HttpPathParameter("id") String id, LineItemRepository lineItems,
            ObjectResponse<List<LineItem>> response) {
        response.send(lineItems.findByInvoiceIdOrderByIdAsc(Long.valueOf(id)));
    }
}
