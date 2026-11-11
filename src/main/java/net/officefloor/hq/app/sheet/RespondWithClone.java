package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClone {
    public void respond(@Val Clone clone, ObjectResponse<CloneResponse> response) {
        response.send(new CloneResponse(clone.to().id()));
    }
}
