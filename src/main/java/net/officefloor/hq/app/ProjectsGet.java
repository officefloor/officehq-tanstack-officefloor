package net.officefloor.hq.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects — every project with its client's name and the ids of its labels, oldest id
 * first. Wired by {@code officefloor/rest/api/projects.GET.yml}. The label ids ride along so the
 * list can filter by label without a request per row.
 */
public class ProjectsGet {

    public void service(ProjectRepository projects, ProjectTagRepository projectTags,
            ObjectResponse<List<ProjectView>> response) {
        Map<Long, List<Long>> tagIdsByProject = new HashMap<>();
        for (ProjectTag pairing : projectTags.findAll()) {
            tagIdsByProject.computeIfAbsent(pairing.getProjectId(), key -> new ArrayList<>())
                    .add(pairing.getTagId());
        }
        List<ProjectView> views = new ArrayList<>();
        for (ProjectView view : projects.findAllViews()) {
            views.add(view.withTagIds(tagIdsByProject.getOrDefault(view.getId(), List.of())));
        }
        response.send(views);
    }
}
