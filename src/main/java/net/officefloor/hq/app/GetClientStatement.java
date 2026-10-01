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
            ClientRepository clients, ProjectRepository projects, InvoiceRepository invoices,
            InvoicePaymentRepository payments,
            ObjectResponse<ClientStatementView> response) {
        Long id = Long.valueOf(clientId);
        // The statement is for one client, so every figure on it is shown in that client's currency.
        String currency = clients.findById(id).map(Client::getCurrency).orElse("USD");
        List<StatementInvoiceView> rows = new ArrayList<>();
        List<StatementProjectView> groups = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        // The client owns projects; each project owns invoices. Walk the client's projects (id order)
        // and collect every invoice, so the statement puts them all in one place AND groups them by
        // job — each group carrying its own subtotal (the sum of its invoices' dues).
        for (Project project : projects.findByClientIdOrderByIdAsc(id)) {
            List<StatementInvoiceView> projectRows = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (Invoice invoice : invoices.findByProjectIdOrderByIdAsc(project.getId())) {
                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                        .map(InvoicePayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal due = invoice.getTotal().subtract(paid);
                StatementInvoiceView row = new StatementInvoiceView(invoice.getId(),
                        invoice.getProjectId(), invoice.getStatus(), invoice.getAmount(), due);
                rows.add(row);
                projectRows.add(row);
                subtotal = subtotal.add(due);
                total = total.add(due);
            }
            groups.add(new StatementProjectView(project.getId(), project.getName(), subtotal,
                    projectRows));
        }
        rows.sort((a, b) -> Long.compare(a.id(), b.id()));
        response.send(new ClientStatementView(id, rows, groups, total, currency));
    }
}
