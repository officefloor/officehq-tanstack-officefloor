package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/invoices/all?status=&lt;stage&gt;&amp;page=&lt;n&gt;&amp;pageSize=&lt;n&gt; — the invoices
 * across every project, in id order, one PAGE at a time, each row carrying the NAME of the project it
 * is for and its lifecycle stage (status). One place listing all invoices, so it injects both
 * repositories and joins the project name on the server. The optional {@code status} narrows the list
 * to a single stage (DRAFT, SENT, PAID); blank means every stage. {@code page} (1-based) and
 * {@code pageSize} slice the already-narrowed, already-ordered list at the source, so the client gets
 * just the window it shows — blank/absent means the first page of ten. Wired by
 * officefloor/rest/api/invoices/all.GET.yml.
 */
public class ListAllInvoices {

    private static final int DEFAULT_PAGE_SIZE = 10;

    public void service(@RequestParam("status") String status,
            @RequestParam("page") String page, @RequestParam("pageSize") String pageSize,
            InvoiceRepository invoices, ProjectRepository projects,
            ObjectResponse<List<AllInvoiceView>> response) {
        Map<Long, String> names = projects.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        boolean all = status == null || status.isBlank();
        List<AllInvoiceView> matching = invoices.findAll().stream()
                .filter(inv -> all || status.equals(inv.getStatus()))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(inv -> AllInvoiceView.of(inv, names.getOrDefault(inv.getProjectId(), "")))
                .toList();

        int size = positiveOr(pageSize, DEFAULT_PAGE_SIZE);
        int pageNumber = positiveOr(page, 1);
        int from = Math.min((pageNumber - 1) * size, matching.size());
        int to = Math.min(from + size, matching.size());
        response.send(matching.subList(from, to));
    }

    /** Parse a positive int from the raw query value, falling back when it is blank or malformed. */
    private static int positiveOr(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
