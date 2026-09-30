package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects — create a project from {name, clientId} and return the saved row (with its
 * id). Wired by {@code officefloor/rest/api/projects.POST.yml}. A project must have a name and
 * belong to an existing client; a missing name or client id is rejected with 400.
 */
public class ProjectsPost {

    public void service(@RequestBody NewProject body, ProjectRepository projects,
            ClientRepository clients, ObjectResponse<Project> response) {
        String name = body.getName();
        Long clientId = body.getClientId();
        if (name == null || name.trim().isEmpty() || clientId == null
                || !clients.existsById(clientId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Project project = new Project();
        project.setName(name.trim());
        project.setClientId(clientId);
        response.send(projects.save(project));
    }
}
