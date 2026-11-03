package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithCrews {
    public void respond(@Val Crews crews, ObjectResponse<Crews> response) {
        response.send(crews);
    }
}
