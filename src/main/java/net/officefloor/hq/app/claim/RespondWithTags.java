package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Every tag ever given to a claim or person on the sheet, sorted. */
public class RespondWithTags {
    public void respond(@Val Tags tags, ObjectResponse<List<String>> response) {
        response.send(tags.list());
    }
}
