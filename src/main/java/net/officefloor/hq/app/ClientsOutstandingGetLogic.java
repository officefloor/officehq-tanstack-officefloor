package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients/outstanding} — how much every client still owes, in one place, so the
 * clients list can be ordered by it. For each client it sums the due across ALL of that client's
 * invoices (invoice -&gt; project -&gt; client), each invoice's due being its net total after any
 * discount/tax (the one-place {@link InvoiceMoney} rule) minus what has been paid against it — the
 * same money math as {@link ClientStatementGetLogic}, worked out on the server so every surface
 * agrees. Every client gets a row, zero when nothing is owed. Wired by
 * {@code officefloor/rest/api/clients/outstanding.GET.yml}.
 */
public class ClientsOutstandingGetLogic {

    public void service(ClientRepository clients, ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<List<ClientOutstandingView>> response) {
        // invoice -> project -> client: map each project to the client it belongs to.
        Map<Long, Long> projectToClient = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getClientId));
        List<ClientOutstandingView> rows = clients.findAll().stream()
                .map(client -> {
                    BigDecimal outstanding = invoices.findAll().stream()
                            .filter(i -> client.getId().equals(projectToClient.get(i.getProjectId())))
                            .map(i -> {
                                BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(i.getId())
                                        .stream().map(Payment::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                BigDecimal netTotal = InvoiceMoney.netTotal(i.getAmount(),
                                        i.getDiscountPct(), i.getTaxPct());
                                return netTotal.subtract(paid);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ClientOutstandingView(client.getId(), outstanding);
                })
                .collect(Collectors.toList());
        response.send(rows);
    }

    /** How much one client still owes across all of its invoices. */
    public static class ClientOutstandingView {
        private final long clientId;
        private final BigDecimal outstanding;

        public ClientOutstandingView(long clientId, BigDecimal outstanding) {
            this.clientId = clientId;
            this.outstanding = outstanding;
        }

        public long getClientId() {
            return clientId;
        }

        public BigDecimal getOutstanding() {
            return outstanding;
        }
    }
}
