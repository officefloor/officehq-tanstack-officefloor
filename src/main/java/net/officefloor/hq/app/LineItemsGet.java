package net.officefloor.hq.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/lineitems} — list every line item, oldest first, each carrying the id of the
 * invoice it belongs to so the invoice-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/lineitems.GET.yml}. Each line also carries the currency it is shown in
 * — the billing currency of the client its invoice's project belongs to — so the invoice-detail line
 * amounts read in the same currency as the rest of the invoice.
 */
public class LineItemsGet {

    public void service(LineItemRepository lineItems, InvoiceRepository invoices,
            ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<LineItem>> response) {
        // A line knows its invoice, an invoice its project, a project its client — so map each line's
        // invoice back to that client's currency.
        Map<Long, String> currencyByProject = Currencies.byProject(projects, clients);
        Map<Long, String> currencyByInvoice = new HashMap<>();
        for (Invoice invoice : invoices.findAllByOrderByIdAsc()) {
            currencyByInvoice.put(invoice.getId(),
                    currencyByProject.getOrDefault(invoice.getProjectId(), Currencies.DEFAULT));
        }
        List<LineItem> lines = lineItems.findAllByOrderByIdAsc();
        for (LineItem line : lines) {
            line.setCurrency(currencyByInvoice.getOrDefault(line.getInvoiceId(), Currencies.DEFAULT));
        }
        response.send(lines);
    }
}
