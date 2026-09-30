package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/project-tags} — put a tag on a project and return the pairing. Wired by
 * {@code officefloor/rest/api/project-tags.POST.yml}. Both the project and the tag must exist;
 * applying a tag already on the project is a no-op (the UNIQUE join keeps it single). The change is
 * audited through {@link Audit} so the labelling can be checked back later (the UI shows only the
 * current chips).
 */
public class ProjectTagsPost {

    public void service(@RequestBody ProjectTagRef body, ProjectTagRepository projectTags,
            ProjectRepository projects, TagRepository tags, Audit audit,
            ObjectResponse<ProjectTagView> response) {
        Long projectId = body.getProjectId();
        Project project = projectId == null ? null : projects.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("a project tag requires an existing project");
        }
        Long tagId = body.getTagId();
        Tag tag = tagId == null ? null : tags.findById(tagId).orElse(null);
        if (tag == null) {
            throw new IllegalArgumentException("a project tag requires an existing tag");
        }
        if (!projectTags.existsByProjectIdAndTagId(projectId, tagId)) {
            ProjectTag pairing = new ProjectTag();
            pairing.setProjectId(projectId);
            pairing.setTagId(tagId);
            projectTags.save(pairing);
            audit.record("PROJECT_TAGGED project=" + project.getName() + " tag=" + tag.getName());
        }
        response.send(new ProjectTagView(projectId, tagId, tag.getName()));
    }
}
