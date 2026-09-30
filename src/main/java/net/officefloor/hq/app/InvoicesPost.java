package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices — create an invoice from {projectId, amount} and return the saved row (with
 * its id). Wired by {@code officefloor/rest/api/invoices.POST.yml}. An invoice must have a
 * positive amount (more than zero) and belong to an existing project; a missing/invalid amount or
 * project id is rejected with 400.
 */
public class InvoicesPost {

    public void service(@RequestBody NewInvoice body, InvoiceRepository invoices,
            ProjectRepository projects, ObjectResponse<Invoice> response) {
        Long projectId = body.getProjectId();
        BigDecimal amount = body.getAmount();
        if (projectId == null || !projects.existsById(projectId) || amount == null
                || amount.signum() <= 0) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Invoice invoice = new Invoice();
        invoice.setProjectId(projectId);
        invoice.setAmount(amount);
        response.send(invoices.save(invoice));
    }
}
