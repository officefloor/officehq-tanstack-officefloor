package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/{clientId}/statement — one client's statement: every invoice raised for the
 * client (across all their projects), oldest id first, each with how much is still due, plus the
 * total the client still owes (the sum of those dues). The invoices are also grouped by job — one
 * {@link StatementProject} per project (oldest project id first), each carrying that job's subtotal
 * — so the statement can present the invoices under their job. Wired by
 * {@code officefloor/rest/api/clients/{clientId}/statement.GET.yml}.
 */
public class ClientStatementGet {

    public void service(@HttpPathParameter("clientId") String clientId,
            ProjectRepository projectsRepo, InvoiceRepository invoices, PaymentRepository payments,
            ClientRepository clients, ObjectResponse<ClientStatement> response) {
        long client = Long.valueOf(clientId);
        // The client's currency, looked up once — every invoice on the statement renders in it.
        String currency = clients.findById(client).map(Client::getCurrency).orElse("USD");
        List<ProjectInvoice> rows = new ArrayList<>();
        List<StatementProject> groups = new ArrayList<>();
        BigDecimal totalOwed = BigDecimal.ZERO;
        // Walk the client's projects (oldest first) and, under each, its invoices — so the statement
        // groups the invoices by job with a per-job subtotal. The flat list and total are the same
        // figures as before, just accumulated as we go.
        for (ProjectView project : projectsRepo.findViewsByClientId(client)) {
            List<ProjectInvoice> jobRows = new ArrayList<>();
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                ProjectInvoice row = new ProjectInvoice(invoice,
                        payments.sumByInvoiceId(invoice.getId()), currency);
                jobRows.add(row);
                rows.add(row);
                totalOwed = totalOwed.add(row.getDue());
            }
            groups.add(new StatementProject(project.getId(), project.getName(), jobRows));
        }
        response.send(new ClientStatement(groups, rows, totalOwed, currency));
    }
}
