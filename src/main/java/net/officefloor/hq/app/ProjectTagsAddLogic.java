package net.officefloor.hq.app;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.ObjectResponse;

/**
 * {@code POST /api/project-tags/add} — put a tag on a project from a {projectId, tagId} body and
 * return the project's updated tag list. Wired by {@code officefloor/rest/api/project-tags/add.POST.yml}.
 * The tag must exist; a project is never tagged twice with the same tag (the add is a no-op then).
 * Appends one {@code PROJECT_TAGGED} audit record when the tag is newly added, so the grouping can be
 * checked back later through the audit file.
 */
public class ProjectTagsAddLogic {

    public void service(@RequestBody ProjectTagChange body, ProjectTagRepository projectTags,
            TagRepository tags, Audit audit, ObjectResponse<List<Tag>> response) {
        Long projectId = body.getProjectId();
        Long tagId = body.getTagId();
        if (projectId == null || tagId == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "A projectId and tagId are required");
        }
        if (!tags.existsById(tagId)) {
            throw new HttpException(HttpStatus.NOT_FOUND, "No such tag");
        }
        if (!projectTags.existsByProjectIdAndTagId(projectId, tagId)) {
            projectTags.save(new ProjectTag(projectId, tagId));
            audit.record("PROJECT_TAGGED project=" + projectId + " tag=" + tagId);
        }
        response.send(assignedTags(projectId, projectTags, tags));
    }

    /** The {@link Tag}s assigned to a project, in assignment order — shared by the GET/add/remove. */
    static List<Tag> assignedTags(Long projectId, ProjectTagRepository projectTags,
            TagRepository tags) {
        return projectTags.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(link -> tags.findById(link.getTagId()).orElse(null))
                .filter(tag -> tag != null)
                .toList();
    }

    /** Request body for adding or removing a tag on a project. */
    public static class ProjectTagChange {
        private Long projectId;
        private Long tagId;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public Long getTagId() {
            return tagId;
        }

        public void setTagId(Long tagId) {
            this.tagId = tagId;
        }
    }
}
