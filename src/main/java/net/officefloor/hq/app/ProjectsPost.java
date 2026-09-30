package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/projects} — add a project (name + client) and return the created row with its
 * client's NAME. Wired by {@code officefloor/rest/api/projects.POST.yml}. The client must exist; the
 * create is audited through {@link Audit}.
 */
public class ProjectsPost {

    public void service(@RequestBody NewProject body, ProjectRepository projects,
            ClientRepository clients, Audit audit, ObjectResponse<ProjectView> response) {
        String name = body.getName() == null ? "" : body.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("a project requires a name");
        }
        Long clientId = body.getClientId();
        Client client = clientId == null ? null : clients.findById(clientId).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("a project requires an existing client");
        }
        Project project = new Project();
        project.setName(name);
        project.setClientId(clientId);
        Project saved = projects.save(project);
        audit.record("PROJECT_CREATED id=" + saved.getId() + " name=" + saved.getName()
                + " client=" + client.getName());
        response.send(new ProjectView(saved.getId(), saved.getName(), saved.getClientId(),
                client.getName(), saved.isArchived()));
    }
}
