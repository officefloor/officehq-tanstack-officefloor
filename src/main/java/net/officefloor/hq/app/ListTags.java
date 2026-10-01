package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/tags — every tag in the catalogue, in id order. The add-tag select on a project draws its
 * options from this list (minus the ones already on the project, filtered client-side). Wired by
 * officefloor/rest/api/tags.GET.yml.
 */
public class ListTags {

    public void service(TagRepository tags, ObjectResponse<List<TagView>> response) {
        List<TagView> view = tags.findAllByOrderByIdAsc().stream().map(TagView::of).toList();
        response.send(view);
    }
}
