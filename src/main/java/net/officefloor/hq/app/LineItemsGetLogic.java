package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/lineitems?invoiceId=<id>} — list the line items that belong to one invoice, oldest
 * first, so an invoice's detail page shows only its own charges (description, qty, unit price). Wired
 * by {@code officefloor/rest/api/lineitems.GET.yml}.
 */
public class LineItemsGetLogic {

    public void service(@RequestParam("invoiceId") Long invoiceId, LineItemRepository lineItems,
            ObjectResponse<List<LineItem>> response) {
        response.send(lineItems.findByInvoiceIdOrderByIdAsc(invoiceId));
    }
}
