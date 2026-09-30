package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/invoices/all} — one place listing every invoice across all projects, oldest
 * first, each carrying the NAME of the project it belongs to and its lifecycle status so the
 * all-invoices page shows the project (not its id) and the stage it is at. Wired by
 * {@code officefloor/rest/api/invoices/all.GET.yml}. Joins {@link Invoice} against {@link Project}
 * server-side, mirroring {@link ProjectsGet} joining the client's name.
 */
public class InvoicesAllGet {

    public void service(@HttpQueryParameter("status") String status, InvoiceRepository invoices,
            ProjectRepository projects, ObjectResponse<List<InvoiceView>> response) {
        Map<Long, String> nameById = projects.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        String wanted = status == null ? null : status.trim();
        List<Invoice> filtered = (wanted == null || wanted.isEmpty())
                ? invoices.findAllByOrderByIdAsc()
                : invoices.findByStatusOrderByIdAsc(wanted);
        List<InvoiceView> views = filtered.stream()
                .map(i -> new InvoiceView(i.getId(), i.getProjectId(),
                        nameById.get(i.getProjectId()), i.getAmount(), i.getStatus(),
                        i.getIssuedDate(), i.getDueDate()))
                .collect(Collectors.toList());
        response.send(views);
    }
}
