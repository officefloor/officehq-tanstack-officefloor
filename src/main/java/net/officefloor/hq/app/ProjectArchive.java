package net.officefloor.hq.app;

import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/projects/{projectId}/archive — tuck a project away instead of deleting it: mark it
 * archived so it drops off the lists but nothing is lost, and return the updated row. Wired by
 * {@code officefloor/rest/api/projects/{projectId}/archive.POST.yml}. Archiving is an audited
 * side-effect: one {@code PROJECT_ARCHIVED id=<id>} record is appended per archive so it can be
 * checked back later (CLAUDE.md — audited behaviour goes through {@link Audit}). An unknown project
 * id is rejected with 404.
 */
public class ProjectArchive {

    public void service(@HttpPathParameter("projectId") String projectId,
            ProjectRepository projects, Audit audit, ObjectResponse<Project> response) {
        Project project = projects.findById(Long.valueOf(projectId))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND));
        project.setArchived(true);
        projects.save(project);
        audit.record("PROJECT_ARCHIVED id=" + project.getId());
        response.send(project);
    }
}
