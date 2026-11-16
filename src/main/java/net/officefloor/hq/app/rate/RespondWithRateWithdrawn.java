package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRateWithdrawn {
    public void respond(@Val RateWithdrawal withdrawal, ObjectResponse<RateWithdrawal> response) {
        response.send(withdrawal);
    }
}
