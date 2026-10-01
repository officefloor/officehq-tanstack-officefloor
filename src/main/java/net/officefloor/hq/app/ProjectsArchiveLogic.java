package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/projects/archive} — tuck a project away instead of deleting it, from an {id}
 * body, and return the id that was archived. Wired by
 * {@code officefloor/rest/api/projects/archive.POST.yml}. The project must exist (404 otherwise).
 * The project is kept (nothing is lost): its {@code archived} flag is set so it drops off the lists
 * but can be revealed again. One {@code PROJECT_ARCHIVED} audit record is appended so the archiving
 * can be checked back later through the audit file even though the UI only drops the row.
 */
public class ProjectsArchiveLogic {

    public void service(@RequestBody ArchiveProject body, ProjectRepository projects, Audit audit,
            ObjectResponse<ArchivedProject> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A project id is required");
        }
        Project project = projects.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such project"));
        project.setArchived(true);
        projects.save(project);
        audit.record("PROJECT_ARCHIVED id=" + id);
        response.send(new ArchivedProject(id));
    }

    /** Request body for archiving a project. */
    public static class ArchiveProject {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** The id of the project that was archived. */
    public static class ArchivedProject {
        private final Long id;

        public ArchivedProject(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
