package net.officefloor.hq.app.rollover;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRollover {
    public void respond(@Val RolloverResponse result, ObjectResponse<RolloverResponse> response) {
        response.send(result);
    }
}
