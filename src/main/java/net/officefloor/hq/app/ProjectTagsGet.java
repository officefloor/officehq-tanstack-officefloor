package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/project-tags} — list every project↔tag pairing with the tag's name resolved, so the
 * UI can render each project's tag chips (scoped to one project on the client). Wired by
 * {@code officefloor/rest/api/project-tags.GET.yml}.
 */
public class ProjectTagsGet {

    public void service(ProjectTagRepository projectTags, TagRepository tags,
            ObjectResponse<List<ProjectTagView>> response) {
        Map<Long, String> names = tags.findAll().stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        List<ProjectTagView> views = projectTags.findAllByOrderByIdAsc().stream()
                .map(pt -> new ProjectTagView(pt.getProjectId(), pt.getTagId(),
                        names.get(pt.getTagId())))
                .collect(Collectors.toList());
        response.send(views);
    }
}
