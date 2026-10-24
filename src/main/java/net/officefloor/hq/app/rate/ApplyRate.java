package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the grade's rate from the from-date on the sheet, keeping the one it had for that from-date as an older version,
 *  and logs it with the sheet's today (so the rates can be worked out as they stood on an earlier date). */
public class ApplyRate {
    public void apply(@Val RateInput input, @Val Sheet sheet, RateRepository repository, RateLogRepository log,
            RateVersionRepository versions, Out<Rate> rateOut) {
        rateOut.set(RateRules.set(input, sheet, repository, log, versions));
    }
}
