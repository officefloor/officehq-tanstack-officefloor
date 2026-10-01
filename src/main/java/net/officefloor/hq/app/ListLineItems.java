package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/lineitems?invoiceId=&lt;id&gt; — the line items of one invoice, in id order. The
 * invoice detail page lists ITS lines and works the total out from them, so the invoice id arrives
 * as a query parameter. Wired by officefloor/rest/api/invoices/lineitems.GET.yml.
 */
public class ListLineItems {

    public void service(@RequestParam("invoiceId") String invoiceId,
            InvoiceLineItemRepository lineItems, ObjectResponse<List<LineItemView>> response) {
        Long id = Long.valueOf(invoiceId);
        List<LineItemView> view = lineItems.findByInvoiceIdOrderByIdAsc(id).stream()
                .map(LineItemView::of).toList();
        response.send(view);
    }
}
