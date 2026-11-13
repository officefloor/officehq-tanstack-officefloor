package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    public void respond(@Val RolloverUndoResponse result, ObjectResponse<RolloverUndoResponse> response) {
        response.send(result);
    }
}
