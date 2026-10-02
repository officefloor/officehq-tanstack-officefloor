package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices?projectId=<id>&sort=<order>} — list the invoices that belong to one
 * project, so a project's detail page shows only its own. The optional {@code sort} orders the rows:
 * {@code due} returns them earliest due date first; anything else (the default) keeps id order. Each
 * row's status is WORKED OUT from its payments (see {@link InvoiceStatus}) rather than a hand-set
 * flag — SENT while unpaid, PARTIAL once part paid, PAID once covered. Wired by
 * {@code officefloor/rest/api/invoices.GET.yml}.
 */
public class InvoicesGetLogic {

    public void service(@RequestParam("projectId") Long projectId,
            @RequestParam(value = "sort", required = false) String sort, InvoiceRepository invoices,
            PaymentRepository payments, ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<InvoiceView>> response) {
        // The money is shown in the owning client's currency (invoice -> project -> client). Every
        // invoice in this list belongs to the one project, so the currency is resolved once.
        String currency = projects.findById(projectId)
                .map(Project::getClientId)
                .flatMap(clients::findById)
                .map(Client::getCurrency)
                .orElse("USD");
        List<Invoice> rows = "due".equals(sort)
                ? invoices.findByProjectIdOrderByDueDateAscIdAsc(projectId)
                : invoices.findByProjectIdOrderByIdAsc(projectId);
        List<InvoiceView> views = rows.stream().map(invoice -> {
            BigDecimal paid = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            String status = InvoiceStatus.derive(invoice.getStatus(), invoice.getAmount(), paid);
            return new InvoiceView(invoice.getId(), invoice.getProjectId(), invoice.getAmount(),
                    status, invoice.getIssuedDate(), invoice.getDueDate(), currency);
        }).collect(Collectors.toList());
        response.send(views);
    }

    /** An invoice as the list needs it, with its status worked out from the payments recorded. */
    public static class InvoiceView {
        private final Long id;
        private final Long projectId;
        private final BigDecimal amount;
        private final String status;
        private final LocalDate issuedDate;
        private final LocalDate dueDate;
        private final String currency;

        public InvoiceView(Long id, Long projectId, BigDecimal amount, String status,
                LocalDate issuedDate, LocalDate dueDate, String currency) {
            this.id = id;
            this.projectId = projectId;
            this.amount = amount;
            this.status = status;
            this.issuedDate = issuedDate;
            this.dueDate = dueDate;
            this.currency = currency;
        }

        public Long getId() {
            return id;
        }

        public Long getProjectId() {
            return projectId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public LocalDate getIssuedDate() {
            return issuedDate;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public String getCurrency() {
            return currency;
        }
    }
}
