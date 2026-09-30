package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/clients/{clientId}/projects — every project belonging to one client, oldest id first.
 * Wired by {@code officefloor/rest/api/clients/{clientId}/projects.GET.yml}.
 */
public class ClientProjectsGet {

    public void service(@HttpPathParameter("clientId") String clientId,
            ProjectRepository projects, ObjectResponse<List<ProjectView>> response) {
        response.send(projects.findViewsByClientId(Long.valueOf(clientId)));
    }
}
