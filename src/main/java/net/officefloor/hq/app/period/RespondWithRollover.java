package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRollover {
    public void respond(@Val RolloverResult result, ObjectResponse<RolloverResult> response) {
        response.send(result);
    }
}
