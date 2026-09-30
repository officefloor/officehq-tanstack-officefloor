package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/projects/{projectId}/delete — delete a project the user no longer needs and return the
 * row that was removed. Wired by {@code officefloor/rest/api/projects/{projectId}/delete.POST.yml}.
 * Deleting is an audited side-effect: one {@code PROJECT_DELETED id=<id>} record is appended per
 * deletion so it can be checked back later (CLAUDE.md — audited behaviour goes through
 * {@link Audit}). An unknown project id is rejected with 404.
 */
public class ProjectDelete {

    public void service(@HttpPathParameter("projectId") String projectId,
            ProjectRepository projects, Audit audit, ObjectResponse<Project> response) {
        Project project = projects.findById(Long.valueOf(projectId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        projects.delete(project);
        audit.record("PROJECT_DELETED id=" + project.getId());
        response.send(project);
    }
}
