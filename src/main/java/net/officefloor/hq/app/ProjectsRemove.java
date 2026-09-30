package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/projects/remove} — delete a project the user no longer needs and return the row
 * that was removed. Wired by {@code officefloor/rest/api/projects/remove.POST.yml}. The project must
 * exist; the deletion is audited through {@link Audit} so the removal can be checked back later (the
 * UI can only show what still exists).
 */
public class ProjectsRemove {

    public void service(@RequestBody DeleteProject body, ProjectRepository projects, Audit audit,
            ObjectResponse<ProjectView> response) {
        Long id = body.getId();
        Project project = id == null ? null : projects.findById(id).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("no such project");
        }
        ProjectView removed = new ProjectView(project.getId(), project.getName(),
                project.getClientId(), null);
        projects.delete(project);
        audit.record("PROJECT_DELETED id=" + id);
        response.send(removed);
    }
}
