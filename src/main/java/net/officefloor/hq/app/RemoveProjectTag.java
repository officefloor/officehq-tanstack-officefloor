package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * POST /api/projects/tags/remove — take a tag off a project (ungroup it from that label), returning
 * the project's tags after the change. Wired by officefloor/rest/api/projects/tags/remove.POST.yml.
 *
 * We reject a missing project/tag id before writing anything. Removing a tag that is not on the
 * project is a no-op — we just return the current tags.
 */
public class RemoveProjectTag {

    public void service(@RequestBody ProjectTagForm form, TagRepository tags,
            ProjectTagRepository projectTags, ObjectResponse<List<TagView>> response) {
        Long projectId = form.getProjectId();
        Long tagId = form.getTagId();
        if (projectId == null || tagId == null) {
            throw new IllegalArgumentException("A project id and a tag id are required");
        }
        ProjectTagId key = new ProjectTagId(projectId, tagId);
        if (projectTags.existsById(key)) {
            projectTags.deleteById(key);
        }
        List<TagView> view = projectTags.findByProjectIdOrderByTagIdAsc(projectId).stream()
                .map(link -> tags.findById(link.getTagId()).orElse(null))
                .filter(tag -> tag != null)
                .map(TagView::of)
                .toList();
        response.send(view);
    }
}
