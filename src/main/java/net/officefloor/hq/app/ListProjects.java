package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects — every project, in id order, each carrying its client's NAME so the list can
 * show the client without the front-end joining. Wired by officefloor/rest/api/projects.GET.yml.
 */
public class ListProjects {

    public void service(ProjectRepository projects, ClientRepository clients,
            ObjectResponse<List<ProjectView>> response) {
        Map<Long, String> names = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ProjectView> view = projects.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(p -> ProjectView.of(p, names.getOrDefault(p.getClientId(), "")))
                .toList();
        response.send(view);
    }
}
