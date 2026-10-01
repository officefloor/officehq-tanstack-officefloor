package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.ProjectTagsAddLogic.ProjectTagChange;

/**
 * {@code POST /api/project-tags/remove} — take a tag off a project from a {projectId, tagId} body and
 * return the project's updated tag list. Wired by
 * {@code officefloor/rest/api/project-tags/remove.POST.yml}. Removing a tag the project does not carry
 * is a no-op. Appends one {@code PROJECT_UNTAGGED} audit record when an assignment is removed, so the
 * grouping change can be checked back later through the audit file.
 */
public class ProjectTagsRemoveLogic {

    public void service(@RequestBody ProjectTagChange body, ProjectTagRepository projectTags,
            TagRepository tags, Audit audit, ObjectResponse<List<Tag>> response) {
        Long projectId = body.getProjectId();
        Long tagId = body.getTagId();
        if (projectId == null || tagId == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A projectId and tagId are required");
        }
        List<ProjectTag> links = projectTags.findByProjectIdAndTagId(projectId, tagId);
        if (!links.isEmpty()) {
            projectTags.deleteAll(links);
            audit.record("PROJECT_UNTAGGED project=" + projectId + " tag=" + tagId);
        }
        response.send(ProjectTagsAddLogic.assignedTags(projectId, projectTags, tags));
    }
}
