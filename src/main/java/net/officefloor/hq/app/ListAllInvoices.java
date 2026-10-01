package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/all?status=&lt;stage&gt; — every invoice across every project, in id order, each
 * carrying the NAME of the project it is for and its lifecycle stage (status). One place listing all
 * invoices, so it injects both repositories and joins the project name on the server. The optional
 * {@code status} narrows the list to a single stage (DRAFT, SENT, PAID); blank means every stage.
 * Wired by officefloor/rest/api/invoices/all.GET.yml.
 */
public class ListAllInvoices {

    public void service(@RequestParam("status") String status,
            InvoiceRepository invoices, ProjectRepository projects,
            ObjectResponse<List<AllInvoiceView>> response) {
        Map<Long, String> names = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        boolean all = status == null || status.isBlank();
        List<AllInvoiceView> view = invoices.findAll().stream()
                .filter(inv -> all || status.equals(inv.getStatus()))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(inv -> AllInvoiceView.of(inv, names.getOrDefault(inv.getProjectId(), "")))
                .toList();
        response.send(view);
    }
}
