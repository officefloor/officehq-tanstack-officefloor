package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTree {
    public void respond(@Val Tree tree, ObjectResponse<List<String>> response) {
        response.send(tree.ids());
    }
}
