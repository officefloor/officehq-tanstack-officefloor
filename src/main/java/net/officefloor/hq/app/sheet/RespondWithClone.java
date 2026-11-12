package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClone {
    public void respond(@Val CloneRequest request, ObjectResponse<CloneResponse> response) {
        response.send(new CloneResponse(request.to()));
    }
}
