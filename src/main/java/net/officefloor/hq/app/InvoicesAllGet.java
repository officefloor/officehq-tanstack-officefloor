package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/all} — one place listing every invoice across all projects, oldest
 * first, each carrying the NAME of the project it belongs to and its lifecycle status so the
 * all-invoices page shows the project (not its id) and the stage it is at. Wired by
 * {@code officefloor/rest/api/invoices/all.GET.yml}. Joins {@link Invoice} against {@link Project}
 * server-side, mirroring {@link ProjectsGet} joining the client's name.
 *
 * <p>The list can grow huge, so it is served a page at a time: {@code page} (1-based, default 1)
 * and {@code size} (default 10) query parameters select the slice, and only that slice is fetched
 * from the database. The front-end's next/previous controls step {@code page}; the ordering stays
 * oldest-first so paging only windows the list, never reorders it.
 */
public class InvoicesAllGet {

    /** How many invoices a page shows when the client does not ask for a specific size. */
    private static final int DEFAULT_PAGE_SIZE = 10;

    public void service(@HttpQueryParameter("status") String status,
            @HttpQueryParameter("page") String page, @HttpQueryParameter("size") String size,
            InvoiceRepository invoices, ProjectRepository projects,
            ObjectResponse<List<InvoiceView>> response) {
        Map<Long, String> nameById = projects.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        String wanted = status == null ? null : status.trim();
        int pageNo = positiveOr(page, 1);
        int pageSize = positiveOr(size, DEFAULT_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        List<Invoice> filtered = (wanted == null || wanted.isEmpty())
                ? invoices.findAll(pageable).getContent()
                : invoices.findByStatus(wanted, pageable).getContent();
        List<InvoiceView> views = filtered.stream()
                .map(i -> new InvoiceView(i.getId(), i.getProjectId(),
                        nameById.get(i.getProjectId()), i.getAmount(), i.getStatus(),
                        i.getIssuedDate(), i.getDueDate()))
                .collect(Collectors.toList());
        response.send(views);
    }

    /** Parse a 1-or-more query parameter, falling back to {@code fallback} when absent or invalid. */
    private static int positiveOr(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 1 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
