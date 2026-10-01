package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/archive — tuck a project away rather than delete it, returning the updated
 * row. Wired by officefloor/rest/api/projects/archive.POST.yml.
 *
 * Archiving keeps the project (and everything hanging off it): it only sets the project's archived
 * flag so it drops off the lists, which default to the not-archived rows. Like deleting, it is an
 * audited side-effect — alongside the change we append one record through the {@link Audit} service
 * ({@code PROJECT_ARCHIVED id=<id>}) so there is a durable note every time a project is tucked away.
 * We reject a missing or unknown project id before writing anything so a bad request neither changes
 * state nor audits.
 */
public class ArchiveProject {

    public void service(@RequestBody ArchiveProjectForm form, ProjectRepository projects,
            ClientRepository clients, Audit audit, ObjectResponse<ProjectView> response) {
        Long id = form.getId();
        if (id == null) {
            throw new IllegalArgumentException("A project id is required");
        }
        Project project = projects.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("A valid project is required"));
        project.archive();
        projects.save(project);
        String clientName = clients.findById(project.getClientId())
                .map(Client::getName).orElse("");
        audit.record("PROJECT_ARCHIVED id=" + id);
        response.send(ProjectView.of(project, clientName));
    }
}
