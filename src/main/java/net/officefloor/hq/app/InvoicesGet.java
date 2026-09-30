package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices} — list every invoice, oldest first, each carrying the id of the project
 * it belongs to so the project-detail view can show just its own. Wired by
 * {@code officefloor/rest/api/invoices.GET.yml}.
 *
 * <p>An optional {@code sort=due} query parameter orders the list by due date (earliest first) —
 * this backs the sort-by-due-date control on the project invoices list. Any other value (or none)
 * keeps the stable oldest-first order.
 */
public class InvoicesGet {

    public void service(@HttpQueryParameter("sort") String sort, InvoiceRepository repository,
            ObjectResponse<List<Invoice>> response) {
        if ("due".equals(sort == null ? null : sort.trim())) {
            response.send(repository.findAllByOrderByDueDateAscIdAsc());
        } else {
            response.send(repository.findAllByOrderByIdAsc());
        }
    }
}
