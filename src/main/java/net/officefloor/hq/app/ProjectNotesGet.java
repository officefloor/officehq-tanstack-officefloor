package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects/{projectId}/notes — every note written on one project, newest first. Wired by
 * {@code officefloor/rest/api/projects/{projectId}/notes.GET.yml}.
 */
public class ProjectNotesGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            NoteRepository notes, ObjectResponse<List<Note>> response) {
        response.send(
                notes.findByTargetTypeAndTargetIdOrderByAtDescIdDesc("project", Long.valueOf(projectId)));
    }
}
