package net.officefloor.hq.app;

import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/projects/remove} — delete a project the owner no longer needs from an {id} body
 * and return the id that was removed. Wired by {@code officefloor/rest/api/projects/remove.POST.yml}.
 * The project must exist (404 otherwise). One {@code PROJECT_DELETED} audit record is appended so the
 * deletion can be checked back later through the audit file even though the UI only drops the row.
 */
public class ProjectsRemoveLogic {

    public void service(@RequestBody RemoveProject body, ProjectRepository projects, Audit audit,
            ObjectResponse<RemovedProject> response) {
        Long id = body.getId();
        if (id == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A project id is required");
        }
        Project project = projects.findById(id)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "No such project"));
        projects.delete(project);
        audit.record("PROJECT_DELETED id=" + id);
        response.send(new RemovedProject(id));
    }

    /** Request body for deleting a project. */
    public static class RemoveProject {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** The id of the project that was removed. */
    public static class RemovedProject {
        private final Long id;

        public RemovedProject(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
