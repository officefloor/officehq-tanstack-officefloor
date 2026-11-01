package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRatesAfterWithdraw {
    /** The sheet's rates as they stand after the withdrawal. */
    public void respond(@Val RateWithdrawal withdrawal, Rates rates, ObjectResponse<List<RateResponse>> response) {
        response.send(rates.onSheet(withdrawal.sheetId()).stream().map(RateResponse::from).toList());
    }
}
