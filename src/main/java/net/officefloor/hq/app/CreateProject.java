package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects — create a project from the submitted name + chosen client id, returning the
 * saved row (with its generated id) and the client's name for the list. Wired by
 * officefloor/rest/api/projects.POST.yml.
 *
 * A project must name a real client: we reject a blank name or an unknown/absent client id before
 * persisting so a bad row can never be saved.
 *
 * A project also carries a short reference code that must be unique across projects: we reject a
 * blank code or one already in use before persisting (the UNIQUE constraint in V32__project_code.sql
 * is the final guard).
 */
public class CreateProject {

    public void service(@RequestBody ProjectForm form, ProjectRepository projects,
            ClientRepository clients, ObjectResponse<ProjectView> response) {
        String name = form.getName() == null ? "" : form.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A project name is required");
        }
        Long clientId = form.getClientId();
        Client client = clientId == null ? null : clients.findById(clientId).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("A valid client is required");
        }
        String code = form.getCode() == null ? "" : form.getCode().trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("A project code is required");
        }
        if (projects.existsByCode(code)) {
            throw new IllegalArgumentException("A project with this code already exists");
        }
        ProjectStatus status = ProjectStatus.parse(form.getStatus());
        Project project = new Project(name, clientId, status);
        project.setCode(code);
        Project saved = projects.save(project);
        response.send(ProjectView.of(saved, client.getName()));
    }
}
