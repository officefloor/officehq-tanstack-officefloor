package net.officefloor.hq.app;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/invoices} — raise an invoice on a project from a {projectId, amount} body and
 * return the saved row (with its generated id). Wired by {@code officefloor/rest/api/invoices.POST.yml}.
 * An invoice must belong to an existing project and carry a positive amount; both are validated here
 * and rejected with 400 (the DB foreign key in Flyway V4 is the matching last line of defence).
 */
public class InvoicesPostLogic {

    public void service(@RequestBody NewInvoice newInvoice, InvoiceRepository invoices,
            ProjectRepository projects, ObjectResponse<Invoice> response) {
        Long projectId = newInvoice.getProjectId();
        BigDecimal amount = newInvoice.getAmount();
        if (projectId == null || !projects.existsById(projectId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid project is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A positive amount is required");
        }
        Invoice invoice = new Invoice(projectId, amount);
        // An invoice goes out today and is due 30 days later; the dates are shown on each row.
        java.time.LocalDate issued = java.time.LocalDate.now();
        invoice.setIssuedDate(issued);
        invoice.setDueDate(issued.plusDays(30));
        Invoice saved = invoices.save(invoice);
        response.send(saved);
    }

    /** Request body for raising an invoice. */
    public static class NewInvoice {
        private Long projectId;
        private BigDecimal amount;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
