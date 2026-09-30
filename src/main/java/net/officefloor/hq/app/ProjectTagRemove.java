package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * POST /api/projects/{projectId}/tags/{tagId}/remove — detach a label from a project and return the
 * project's remaining label set. Wired by
 * {@code officefloor/rest/api/projects/{projectId}/tags/{tagId}/remove.POST.yml}. Removing a pairing
 * that is not there is a no-op; the current set is returned either way.
 */
public class ProjectTagRemove {

    public void service(@HttpPathParameter("projectId") String projectId,
            @HttpPathParameter("tagId") String tagId, TagRepository tags,
            ProjectTagRepository projectTags, ObjectResponse<List<Tag>> response) {
        Long id = Long.valueOf(projectId);
        ProjectTagId pairing = new ProjectTagId(id, Long.valueOf(tagId));
        if (projectTags.existsById(pairing)) {
            projectTags.deleteById(pairing);
        }
        response.send(tags.findByProjectId(id));
    }
}
