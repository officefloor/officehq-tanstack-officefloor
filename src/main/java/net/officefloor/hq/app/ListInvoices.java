package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices?projectId=&lt;id&gt;&amp;sort=&lt;key&gt; — the invoices of one project. Scoped to a
 * project (the detail page lists ITS invoices), so the project id arrives as a query parameter. The
 * optional {@code sort} key chooses the order: {@code due} is earliest-due-first, anything else (and
 * the default) is id order. Wired by officefloor/rest/api/invoices.GET.yml.
 */
public class ListInvoices {

    public void service(@RequestParam("projectId") String projectId,
            @RequestParam("sort") String sort,
            InvoiceRepository invoices, InvoicePaymentRepository payments,
            ObjectResponse<List<InvoiceView>> response) {
        Long id = Long.valueOf(projectId);
        List<Invoice> rows = "due".equals(sort)
                ? invoices.findByProjectIdOrderByDueDateAscIdAsc(id)
                : invoices.findByProjectIdOrderByIdAsc(id);
        // Each row's status is worked out from what has been paid against it, not a stored flag.
        List<InvoiceView> view = rows.stream()
                .map(inv -> InvoiceView.ofDerived(inv, paidSum(payments, inv.getId())))
                .toList();
        response.send(view);
    }

    /** The total paid against one invoice — the sum of its recorded payments (zero if none). */
    private static BigDecimal paidSum(InvoicePaymentRepository payments, Long invoiceId) {
        return payments.findByInvoiceIdOrderByIdAsc(invoiceId).stream()
                .map(InvoicePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
