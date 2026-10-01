package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices?projectId=<id>&sort=<order>} — list the invoices that belong to one
 * project, so a project's detail page shows only its own. The optional {@code sort} orders the rows:
 * {@code due} returns them earliest due date first; anything else (the default) keeps id order.
 * Wired by {@code officefloor/rest/api/invoices.GET.yml}.
 */
public class InvoicesGetLogic {

    public void service(@RequestParam("projectId") Long projectId,
            @RequestParam(value = "sort", required = false) String sort, InvoiceRepository invoices,
            ObjectResponse<List<Invoice>> response) {
        List<Invoice> rows = "due".equals(sort)
                ? invoices.findByProjectIdOrderByDueDateAscIdAsc(projectId)
                : invoices.findByProjectIdOrderByIdAsc(projectId);
        response.send(rows);
    }
}
