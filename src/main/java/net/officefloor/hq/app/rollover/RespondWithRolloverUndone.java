package net.officefloor.hq.app.rollover;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    public void respond(@Val RolloverRecord undone, ObjectResponse<RolloverUndoneResult> response) {
        response.send(new RolloverUndoneResult(undone.fy(), undone.periodNames().size()));
    }
}
