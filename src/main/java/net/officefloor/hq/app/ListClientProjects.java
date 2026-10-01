package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/clients/projects?clientId=&lt;id&gt; — the projects of one client, in id order, each
 * carrying its client's NAME so the row reuses the same {@link ProjectView} shape the projects
 * list renders. Scoped to a client (the client detail page lists ITS projects), so the client id
 * arrives as a query parameter. Wired by officefloor/rest/api/clients/projects.GET.yml.
 */
public class ListClientProjects {

    public void service(@RequestParam("clientId") String clientId,
            ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<ProjectView>> response) {
        Long id = Long.valueOf(clientId);
        String clientName = clients.findById(id).map(Client::getName).orElse("");
        // Archived projects are tucked away — they drop off the client's list too, keeping the row
        // in the table only while it is active.
        List<ProjectView> view = projects.findByClientIdOrderByIdAsc(id).stream()
                .filter(p -> !p.isArchived())
                .map(p -> ProjectView.of(p, clientName))
                .toList();
        response.send(view);
    }
}
