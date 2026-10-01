package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/delete — remove a project, returning the row that was deleted. Wired by
 * officefloor/rest/api/projects/delete.POST.yml.
 *
 * Deleting is an audited side-effect: alongside the delete we append one record to the audit file
 * through the {@link Audit} service ({@code PROJECT_DELETED id=<id>}) so there is a durable record
 * every time a project is removed — the trail the UI can't show. We reject a missing or unknown
 * project id before writing anything so a bad request neither changes state nor audits.
 */
public class DeleteProject {

    public void service(@RequestBody DeleteProjectForm form, ProjectRepository projects,
            ClientRepository clients, Audit audit, ObjectResponse<ProjectView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A project id is required");
        }
        Project project = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid project is required"));
        String clientName = clients.findById(project.getClientId())
                .map(Client::getName).orElse("");
        ProjectView view = ProjectView.of(project, clientName);
        projects.delete(project);
        audit.record("PROJECT_DELETED id=" + id);
        response.send(view);
    }
}
