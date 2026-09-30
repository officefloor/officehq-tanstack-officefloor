package net.officefloor.hq.app;

import java.util.Set;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects — create a project from {name, clientId, status} and return the saved row (with
 * its id). Wired by {@code officefloor/rest/api/projects.POST.yml}. A project must have a name and
 * belong to an existing client; a missing name or client id is rejected with 400. The status is one
 * of ACTIVE/ON_HOLD/FINISHED and defaults to ACTIVE when omitted; an unknown value is rejected.
 */
public class ProjectsPost {

    /** The project lifecycle statuses the form offers and the server accepts. */
    private static final Set<String> STATUSES = Set.of("ACTIVE", "ON_HOLD", "FINISHED");

    public void service(@RequestBody NewProject body, ProjectRepository projects,
            ClientRepository clients, ObjectResponse<Project> response) {
        String name = body.getName();
        Long clientId = body.getClientId();
        if (name == null || name.trim().isEmpty() || clientId == null
                || !clients.existsById(clientId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        String status = body.getStatus() == null || body.getStatus().isEmpty()
                ? "ACTIVE" : body.getStatus();
        if (!STATUSES.contains(status)) {
            throw new HttpException(HttpStatus.BAD_REQUEST);
        }
        Project project = new Project();
        project.setName(name.trim());
        project.setClientId(clientId);
        project.setStatus(status);
        response.send(projects.save(project));
    }
}
