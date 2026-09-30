package net.officefloor.hq.app;

import java.math.BigDecimal;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/invoices} — add an invoice (project + amount) and return the created row. Wired
 * by {@code officefloor/rest/api/invoices.POST.yml}. The project must exist and the amount must be
 * positive; the create is audited through {@link Audit}.
 */
public class InvoicesPost {

    public void service(@RequestBody NewInvoice body, InvoiceRepository invoices,
            ProjectRepository projects, Audit audit, ObjectResponse<Invoice> response) {
        Long projectId = body.getProjectId();
        Project project = projectId == null ? null : projects.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("an invoice requires an existing project");
        }
        BigDecimal amount = body.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("an invoice requires a positive amount");
        }
        Invoice invoice = new Invoice();
        invoice.setProjectId(projectId);
        invoice.setAmount(amount);
        invoice.setStatus("UNPAID");
        Invoice saved = invoices.save(invoice);
        audit.record("INVOICE_CREATED id=" + saved.getId() + " project=" + project.getName()
                + " amount=" + saved.getAmount().toPlainString());
        response.send(saved);
    }
}
