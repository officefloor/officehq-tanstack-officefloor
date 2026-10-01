package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/clients/statement?clientId=&lt;id&gt; — a statement for one client: every invoice across
 * every project the client owns, in id order, each carrying how much is still DUE on it, plus the
 * TOTAL the client still owes (the sum of those dues). Scoped to a client (the statement is for one
 * client), so the client id arrives as a query parameter. The cross-project join and the derived
 * balances live on the server, computed in {@link BigDecimal} so there is no float drift — the same
 * derivation {@link InvoiceAmountDue} applies per invoice. Wired by
 * officefloor/rest/api/clients/statement.GET.yml.
 */
public class GetClientStatement {

    public void service(@RequestParam("clientId") String clientId,
            ProjectRepository projects, InvoiceRepository invoices,
            InvoicePaymentRepository payments,
            ObjectResponse<ClientStatementView> response) {
        Long id = Long.valueOf(clientId);
        List<StatementInvoiceView> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        // The client owns projects; each project owns invoices. Walk the client's projects (id order)
        // and collect every invoice, so the statement puts them all in one place.
        for (Project project : projects.findByClientIdOrderByIdAsc(id)) {
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                        .map(InvoicePayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal due = invoice.getTotal().subtract(paid);
                rows.add(new StatementInvoiceView(invoice.getId(), invoice.getProjectId(),
                        invoice.getStatus(), invoice.getAmount(), due));
                total = total.add(due);
            }
        }
        rows.sort((a, b) -> Long.compare(a.id(), b.id()));
        response.send(new ClientStatementView(id, rows, total));
    }
}
