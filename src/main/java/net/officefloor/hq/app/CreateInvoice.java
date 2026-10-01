package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/invoices — raise an invoice against a project from the submitted amount + project id,
 * returning the saved row (with its generated id). Wired by officefloor/rest/api/invoices.POST.yml.
 *
 * An invoice must name a real project and carry an amount: we reject a missing amount or an
 * unknown/absent project id before persisting so a bad row can never be saved.
 */
public class CreateInvoice {

    public void service(@RequestBody InvoiceForm form, InvoiceRepository invoices,
            ProjectRepository projects, ObjectResponse<InvoiceView> response) {
        BigDecimal amount = form.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("An invoice amount is required");
        }
        Long projectId = form.getProjectId();
        if (projectId == null || projects.findById(projectId).isEmpty()) {
            throw new IllegalArgumentException("A valid project is required");
        }
        Invoice saved = invoices.save(new Invoice(projectId, amount));
        response.send(InvoiceView.of(saved));
    }
}
