package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTree {
    public void respond(@Val Tree tree, ObjectResponse<Tree> response) {
        response.send(tree);
    }
}
