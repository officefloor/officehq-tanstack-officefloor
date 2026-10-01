package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/tags} — list every tag, so the project detail page can offer them in the "add a
 * tag" picker. Wired by {@code officefloor/rest/api/tags.GET.yml}.
 */
public class TagsGetLogic {

    public void service(TagRepository tags, ObjectResponse<List<Tag>> response) {
        response.send(tags.findAllByOrderByIdAsc());
    }
}
