package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GET /api/projects/tags?projectId=&lt;id&gt; — the tags on one project, in tag-id order. Scoped to a
 * project (the detail page lists ITS tags), so the project id arrives as a query parameter. Reads the
 * {@code project_tags} join for this project, then resolves each link to its {@link Tag} so the chip
 * can show the label. Wired by officefloor/rest/api/projects/tags.GET.yml.
 */
public class ListProjectTags {

    public void service(@RequestParam("projectId") String projectId,
            ProjectTagRepository projectTags, TagRepository tags,
            ObjectResponse<List<TagView>> response) {
        Long id = Long.valueOf(projectId);
        List<TagView> view = projectTags.findByProjectIdOrderByTagIdAsc(id).stream()
                .map(link -> tags.findById(link.getTagId()).orElse(null))
                .filter(tag -> tag != null)
                .map(TagView::of)
                .toList();
        response.send(view);
    }
}
