package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices} — list every invoice, oldest first, each carrying the id of the project
 * it belongs to so the project-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/invoices.GET.yml}.
 */
public class InvoicesGet {

    public void service(InvoiceRepository repository, ObjectResponse<List<Invoice>> response) {
        response.send(repository.findAllByOrderByIdAsc());
    }
}
