package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/clients/statement?clientId=N} — one client's statement: all of that client's
 * invoices in one place (every invoice raised against any of the client's projects, oldest first),
 * each carrying its derived {@code amountDue}, plus the {@code outstanding} total the client still
 * owes. Wired by {@code officefloor/rest/api/clients/statement.GET.yml}.
 *
 * <p>Invoices belong to projects, and projects belong to clients, so the client's invoices are
 * those whose project belongs to the client. Each invoice's amount due is its amount minus every
 * payment recorded against it (the same derivation {@link InvoicesGet} makes), and the outstanding
 * total is the sum of those amounts due — a server-derived aggregate, mirroring the dashboard's
 * outstanding total.
 */
public class ClientStatementGet {

    public void service(@HttpQueryParameter("clientId") String clientId, ProjectRepository projects,
            InvoiceRepository invoices, PaymentRepository payments,
            ObjectResponse<StatementView> response) {
        Long id = Long.valueOf(clientId.trim());

        // The client's projects: their ids are what ties an invoice back to this client.
        Set<Long> projectIds = projects.findAllByOrderByIdAsc().stream()
                .filter(p -> id.equals(p.getClientId()))
                .map(Project::getId)
                .collect(Collectors.toSet());

        // Sum the payments per invoice once, so each invoice's amount due (amount less what has been
        // paid) can be worked out without re-scanning the payments per row.
        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        for (Payment payment : payments.findAllByOrderByIdAsc()) {
            paidByInvoice.merge(payment.getInvoiceId(), payment.getAmount(), BigDecimal::add);
        }

        List<Invoice> clientInvoices = invoices.findAllByOrderByIdAsc().stream()
                .filter(i -> projectIds.contains(i.getProjectId()))
                .collect(Collectors.toList());

        BigDecimal outstanding = BigDecimal.ZERO;
        for (Invoice invoice : clientInvoices) {
            BigDecimal paid = paidByInvoice.getOrDefault(invoice.getId(), BigDecimal.ZERO);
            // Owed = the discounted amount (subtotal minus the invoice's discount, the same figure the
            // invoice detail and dashboard use) less what has been paid, so the discount shows up in
            // the statement's amount due and outstanding total too.
            BigDecimal due = invoice.getDiscountedAmount().subtract(paid);
            invoice.setAmountDue(due);
            outstanding = outstanding.add(due);
        }

        response.send(new StatementView(clientInvoices, outstanding));
    }
}
