package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/invoices/{projectId} — every invoice for one project, oldest id first, each carrying how
 * much has been paid and how much is still due (amount - payments). Wired by
 * {@code officefloor/rest/api/invoices/{projectId}.GET.yml}.
 */
public class InvoicesGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            InvoiceRepository invoices, PaymentRepository payments, ProjectRepository projects,
            ClientRepository clients, ObjectResponse<List<ProjectInvoice>> response) {
        // Every invoice in one project belongs to the same client, so their currency is looked up once
        // (the client's, via the project) and carried on each row so the amounts render in it.
        String currency = projects.findById(Long.valueOf(projectId))
                .flatMap(project -> clients.findById(project.getClientId()))
                .map(Client::getCurrency).orElse("USD");
        List<ProjectInvoice> result = new ArrayList<>();
        for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(Long.valueOf(projectId))) {
            result.add(new ProjectInvoice(invoice, payments.sumByInvoiceId(invoice.getId()),
                    currency));
        }
        response.send(result);
    }
}
