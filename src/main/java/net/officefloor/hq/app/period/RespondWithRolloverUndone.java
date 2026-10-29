package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRolloverUndone {
    public void respond(@Val MadeRollover undone, ObjectResponse<RolloverUndoneResponse> response) {
        response.send(new RolloverUndoneResponse(undone.fy(), undone.periods().size()));
    }
}
