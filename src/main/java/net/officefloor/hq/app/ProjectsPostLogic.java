package net.officefloor.hq.app;

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
        Project saved = projects.save(new Project(name.trim(), clientId));
        response.send(saved);
    }

    /** Request body for creating a project. */
    public static class NewProject {
        private String name;
        private Long clientId;

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
    }
}
