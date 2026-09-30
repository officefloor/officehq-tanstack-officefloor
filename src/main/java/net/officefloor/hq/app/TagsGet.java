package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;

/**
 * GET /api/tags — the whole shared pool of labels, oldest id first. Wired by
 * {@code officefloor/rest/api/tags.GET.yml}. The "add a label" picker on a project reads this and
 * drops the ones already attached.
 */
public class TagsGet {

    public void service(TagRepository tags, ObjectResponse<List<Tag>> response) {
        response.send(tags.findAllByOrderByIdAsc());
    }
}
