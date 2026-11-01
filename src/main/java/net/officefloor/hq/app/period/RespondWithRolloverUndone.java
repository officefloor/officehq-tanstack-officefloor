package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    public void respond(@Val RolloverUndone undone, ObjectResponse<RolloverUndone> response) {
        response.send(undone);
    }
}
