package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/projects/archive} — tuck a project away instead of deleting it: flag it archived
 * so it drops off the default project lists while the row (and everything under it) is kept. Wired
 * by {@code officefloor/rest/api/projects/archive.POST.yml}. The project must exist; the archiving
 * is audited through {@link Audit} so it can be checked back later.
 */
public class ProjectsArchive {

    public void service(@RequestBody ArchiveProject body, ProjectRepository projects, Audit audit,
            ObjectResponse<ProjectView> response) {
        Long id = body.getId();
        Project project = id == null ? null : projects.findById(id).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("no such project");
        }
        project.setArchived(true);
        Project saved = projects.save(project);
        audit.record("PROJECT_ARCHIVED id=" + id);
        response.send(new ProjectView(saved.getId(), saved.getName(), saved.getClientId(), null,
                saved.isArchived(), saved.getStatus(), saved.getCode()));
    }
}
