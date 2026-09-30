package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/projects} — list every project, oldest first, each carrying its client's NAME so
 * the UI shows the name (not the id). Wired by {@code officefloor/rest/api/projects.GET.yml}.
 */
public class ProjectsGet {

    public void service(ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<ProjectView>> response) {
        Map<Long, String> nameById = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ProjectView> views = projects.findAllByOrderByIdAsc().stream()
                .map(p -> new ProjectView(p.getId(), p.getName(), p.getClientId(),
                        nameById.get(p.getClientId())))
                .collect(Collectors.toList());
        response.send(views);
    }
}
