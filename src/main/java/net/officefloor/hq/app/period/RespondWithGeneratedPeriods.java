package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGeneratedPeriods {
    public record Generated(int generated) {
    }

    public void respond(@Val GeneratedPeriods generated, ObjectResponse<Generated> response) {
        response.send(new Generated(generated.periods().size()));
    }
}
