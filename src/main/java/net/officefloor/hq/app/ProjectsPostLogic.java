package net.officefloor.hq.app;

import java.util.Set;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/projects} — create a project from a {name, clientId} body and return the saved row
 * (with its generated id). Wired by {@code officefloor/rest/api/projects.POST.yml}. A project must
 * belong to an existing client: the clientId is validated here and rejected with 400 before it
 * reaches the repository (the DB foreign key in Flyway V3 is the matching last line of defence).
 */
public class ProjectsPostLogic {

    /** The lifecycle statuses a project may carry (Flyway V21). */
    private static final Set<String> STATUSES = Set.of("ACTIVE", "ON_HOLD", "FINISHED");

    public void service(@RequestBody NewProject newProject, ProjectRepository projects,
            ClientRepository clients, ObjectResponse<Project> response) {
        String name = newProject.getName();
        Long clientId = newProject.getClientId();
        if (name == null || name.trim().isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A project name is required");
        }
        if (clientId == null || !clients.existsById(clientId)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A valid client is required");
        }
        // Status is optional; default to ACTIVE when absent, and reject anything unrecognised.
        String status = newProject.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        }
        if (!STATUSES.contains(status)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "An unknown project status");
        }
        Project project = new Project(name.trim(), clientId);
        project.setStatus(status);
        Project saved = projects.save(project);
        response.send(saved);
    }

    /** Request body for creating a project. */
    public static class NewProject {
        private String name;
        private Long clientId;
        private String status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
