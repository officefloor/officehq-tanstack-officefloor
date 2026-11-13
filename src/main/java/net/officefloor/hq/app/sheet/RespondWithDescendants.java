package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDescendants {
    public void respond(@Val Descendants descendants, ObjectResponse<List<String>> response) {
        response.send(descendants.ids());
    }
}
