package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices?projectId=&lt;id&gt; — the invoices of one project, in id order. Scoped to a
 * project (the detail page lists ITS invoices), so the project id arrives as a query parameter.
 * Wired by officefloor/rest/api/invoices.GET.yml.
 */
public class ListInvoices {

    public void service(@RequestParam("projectId") String projectId,
            InvoiceRepository invoices, ObjectResponse<List<InvoiceView>> response) {
        Long id = Long.valueOf(projectId);
        List<InvoiceView> view = invoices.findByProjectIdOrderByIdAsc(id).stream()
                .map(InvoiceView::of)
                .toList();
        response.send(view);
    }
}
