package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/tags — put a tag on a project (group the project under that label), returning the
 * project's tags after the change. Wired by officefloor/rest/api/projects/tags.POST.yml.
 *
 * We reject a missing project/tag id, and an unknown project or tag, before writing anything so a bad
 * request neither changes state nor returns a row. Adding a tag that is already on the project is a
 * no-op (the (project, tag) pair is unique) — we just return the current tags.
 */
public class AddProjectTag {

    public void service(@RequestBody ProjectTagForm form, ProjectRepository projects,
            TagRepository tags, ProjectTagRepository projectTags,
            ObjectResponse<List<TagView>> response) {
        Long projectId = form.getProjectId();
        Long tagId = form.getTagId();
        if (projectId == null || tagId == null) {
            throw new IllegalArgumentException("A project id and a tag id are required");
        }
        if (!projects.existsById(projectId)) {
            throw new IllegalArgumentException("A valid project is required");
        }
        if (!tags.existsById(tagId)) {
            throw new IllegalArgumentException("A valid tag is required");
        }
        if (!projectTags.existsById(new ProjectTagId(projectId, tagId))) {
            projectTags.save(new ProjectTag(projectId, tagId));
        }
        List<TagView> view = projectTags.findByProjectIdOrderByTagIdAsc(projectId).stream()
                .map(link -> tags.findById(link.getTagId()).orElse(null))
                .filter(tag -> tag != null)
                .map(TagView::of)
                .toList();
        response.send(view);
    }
}
