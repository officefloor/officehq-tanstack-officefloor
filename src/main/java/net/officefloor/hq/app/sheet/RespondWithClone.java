package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClone {
    public void respond(@Val CloneInput input, ObjectResponse<CloneResponse> response) {
        response.send(new CloneResponse(input.to()));
    }
}
