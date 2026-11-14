package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRateWithdrawn {
    public void respond(@Val RateWithdrawn withdrawn, ObjectResponse<RateWithdrawnResponse> response) {
        response.send(new RateWithdrawnResponse(withdrawn.grade(),
                withdrawn.from() == null ? null : withdrawn.from().toString(), withdrawn.version()));
    }
}
