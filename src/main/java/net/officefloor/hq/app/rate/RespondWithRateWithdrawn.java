package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRateWithdrawn {
    // The grade and from-date withdrawn, with the version that applies again (null when the rate is gone).
    public void respond(@Val WithdrawnRate withdrawn, ObjectResponse<RateWithdrawnResponse> response) {
        response.send(new RateWithdrawnResponse(withdrawn.grade(), withdrawn.from() == null ? null : withdrawn.from().toString(),
                withdrawn.restored() == null ? null : RateResponse.from(withdrawn.restored())));
    }
}
