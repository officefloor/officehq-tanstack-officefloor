package net.officefloor.hq.app;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code POST /api/project-tags/remove} — take a tag off a project and return the pairing that was
 * removed. Wired by {@code officefloor/rest/api/project-tags/remove.POST.yml}. The pairing must
 * exist; the removal is audited through {@link Audit} so it can be checked back later (the UI can
 * only show what still exists).
 */
public class ProjectTagsRemove {

    public void service(@RequestBody ProjectTagRef body, ProjectTagRepository projectTags,
            Audit audit, ObjectResponse<ProjectTagView> response) {
        Long projectId = body.getProjectId();
        Long tagId = body.getTagId();
        ProjectTag pairing = (projectId == null || tagId == null) ? null
                : projectTags.findByProjectIdAndTagId(projectId, tagId).orElse(null);
        if (pairing == null) {
            throw new IllegalArgumentException("no such project tag");
        }
        projectTags.delete(pairing);
        audit.record("PROJECT_UNTAGGED project=" + projectId + " tag=" + tagId);
        response.send(new ProjectTagView(projectId, tagId, null));
    }
}
