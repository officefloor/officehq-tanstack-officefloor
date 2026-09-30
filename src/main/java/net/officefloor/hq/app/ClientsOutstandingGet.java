package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/outstanding — for every client that has not been tucked away, how much they still
 * owe (oldest id first). The outstanding figure is the sum of the dues across all of the client's
 * invoices, computed the same way the client statement does it ({@link ProjectInvoice}), so the list
 * and the statement agree. The clients list reads this alongside {@code /api/clients} so it can be
 * sorted by how much each client owes. Wired by
 * {@code officefloor/rest/api/clients/outstanding.GET.yml}.
 */
public class ClientsOutstandingGet {

    public void service(ClientRepository clients, InvoiceRepository invoices,
            PaymentRepository payments, ObjectResponse<List<ClientOutstanding>> response) {
        List<ClientOutstanding> owed = new ArrayList<>();
        for (Client client : clients.findAllByArchivedFalseOrderByIdAsc()) {
            BigDecimal total = BigDecimal.ZERO;
            for (Invoice invoice : invoices.findByClientId(client.getId())) {
                ProjectInvoice row =
                        new ProjectInvoice(invoice, payments.sumByInvoiceId(invoice.getId()));
                total = total.add(row.getDue());
            }
            owed.add(new ClientOutstanding(client.getId(), total));
        }
        response.send(owed);
    }
}
