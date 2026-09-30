package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/{clientId}/statement — one client's statement: every invoice raised for the
 * client (across all their projects), oldest id first, each with how much is still due, plus the
 * total the client still owes (the sum of those dues). Wired by
 * {@code officefloor/rest/api/clients/{clientId}/statement.GET.yml}.
 */
public class ClientStatementGet {

    public void service(@HttpPathParameter("clientId") String clientId,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<ClientStatement> response) {
        List<ProjectInvoice> rows = new ArrayList<>();
        BigDecimal totalOwed = BigDecimal.ZERO;
        for (Invoice invoice : invoices.findByClientId(Long.valueOf(clientId))) {
            ProjectInvoice row = new ProjectInvoice(invoice, payments.sumByInvoiceId(invoice.getId()));
            rows.add(row);
            totalOwed = totalOwed.add(row.getDue());
        }
        response.send(new ClientStatement(rows, totalOwed));
    }
}
