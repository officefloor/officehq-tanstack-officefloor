package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects — every project with its client's name, oldest id first. Wired by
 * {@code officefloor/rest/api/projects.GET.yml}.
 */
public class ProjectsGet {

    public void service(ProjectRepository projects, ObjectResponse<List<ProjectView>> response) {
        response.send(projects.findAllViews());
    }
}
