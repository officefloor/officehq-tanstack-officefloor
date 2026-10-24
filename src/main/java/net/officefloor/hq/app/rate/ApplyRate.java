package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the grade's rate from the from-date on the sheet, replacing the one it had for that from-date, and logs it
 *  with the sheet's today (so the rates can be worked out as they stood on an earlier date). */
public class ApplyRate {
    public void apply(@Val RateInput input, @Val Sheet sheet, RateRepository repository, RateLogRepository log,
            Out<Rate> rateOut) {
        Rate rate = repository.findWithFrom(sheet.getId(), input.grade(), input.from())
                .orElseGet(() -> new Rate(sheet.getId(), input.grade(), input.pct(), input.from(), input.to()));
        rate.setPct(input.pct());
        rate.setToDate(input.to());
        Rate saved = repository.saveAndFlush(rate);
        log.saveAndFlush(new RateLog(saved, sheet.getToday()));
        rateOut.set(saved);
    }
}
