package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/lineitems} — list every line item, oldest first, each carrying the id of the
 * invoice it belongs to so the invoice-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/lineitems.GET.yml}.
 */
public class LineItemsGet {

    public void service(LineItemRepository lineItems, ObjectResponse<List<LineItem>> response) {
        response.send(lineItems.findAllByOrderByIdAsc());
    }
}
