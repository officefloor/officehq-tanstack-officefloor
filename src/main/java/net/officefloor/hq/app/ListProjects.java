package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects — every project, in id order, each carrying its client's NAME so the list can
 * show the client without the front-end joining, and the ids of the tags on it so the list can be
 * filtered by tag. Wired by officefloor/rest/api/projects.GET.yml.
 */
public class ListProjects {

    public void service(ProjectRepository projects, ClientRepository clients,
            ProjectTagRepository projectTags, ObjectResponse<List<ProjectView>> response) {
        Map<Long, String> names = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        // Which tag ids sit on each project — grouped once from the join so a list of any size is a
        // single scan, not a query per row.
        Map<Long, List<Long>> tagsByProject = projectTags.findAll().stream()
                .collect(Collectors.groupingBy(ProjectTag::getProjectId,
                        Collectors.mapping(ProjectTag::getTagId, Collectors.toList())));
        List<ProjectView> view = projects.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(p -> ProjectView.of(p, names.getOrDefault(p.getClientId(), ""),
                        tagsByProject.getOrDefault(p.getId(), new ArrayList<>())))
                .toList();
        response.send(view);
    }
}
