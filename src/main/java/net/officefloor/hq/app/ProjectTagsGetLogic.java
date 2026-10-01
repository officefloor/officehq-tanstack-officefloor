package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/project-tags?projectId=<id>} — list the tags assigned to one project, oldest
 * assignment first, so the project's detail page shows only its own labels. Wired by
 * {@code officefloor/rest/api/project-tags.GET.yml}.
 */
public class ProjectTagsGetLogic {

    public void service(@RequestParam("projectId") Long projectId, ProjectTagRepository projectTags,
            TagRepository tags, ObjectResponse<List<Tag>> response) {
        response.send(ProjectTagsAddLogic.assignedTags(projectId, projectTags, tags));
    }
}
