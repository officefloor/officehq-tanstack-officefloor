package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * {@code GET /api/tags} — list every tag, oldest first. The UI offers the ones a project does not yet
 * carry as choices to add. Wired by {@code officefloor/rest/api/tags.GET.yml}.
 */
public class TagsGet {

    public void service(TagRepository tags, ObjectResponse<List<Tag>> response) {
        response.send(tags.findAllByOrderByIdAsc());
    }
}
