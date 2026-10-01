package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices?projectId=<id>} — list the invoices that belong to one project, so a
 * project's detail page shows only its own. Wired by {@code officefloor/rest/api/invoices.GET.yml}.
 */
public class InvoicesGetLogic {

    public void service(@RequestParam("projectId") Long projectId, InvoiceRepository invoices,
            ObjectResponse<List<Invoice>> response) {
        response.send(invoices.findByProjectIdOrderByIdAsc(projectId));
    }
}
