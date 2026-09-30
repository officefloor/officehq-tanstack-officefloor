package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{projectId} — every invoice for one project, oldest id first. Wired by
 * {@code officefloor/rest/api/invoices/{projectId}.GET.yml}.
 */
public class InvoicesGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            InvoiceRepository invoices, ObjectResponse<List<Invoice>> response) {
        response.send(invoices.findByProjectIdOrderByIdAsc(Long.valueOf(projectId)));
    }
}
