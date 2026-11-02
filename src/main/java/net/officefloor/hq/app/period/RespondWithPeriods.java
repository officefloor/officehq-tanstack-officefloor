package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val Periods periods, ObjectResponse<Periods> response) {
        response.send(periods);
    }
}
