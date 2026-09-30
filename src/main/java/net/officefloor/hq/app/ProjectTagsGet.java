package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/projects/{projectId}/tags — the labels attached to one project, oldest id first. Wired by
 * {@code officefloor/rest/api/projects/{projectId}/tags.GET.yml}.
 */
public class ProjectTagsGet {

    public void service(@HttpPathParameter("projectId") String projectId,
            TagRepository tags, ObjectResponse<List<Tag>> response) {
        response.send(tags.findByProjectId(Long.valueOf(projectId)));
    }
}
