package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDescendants {
    public void respond(@Val Descendants descendants, ObjectResponse<Descendants> response) {
        response.send(descendants);
    }
}
