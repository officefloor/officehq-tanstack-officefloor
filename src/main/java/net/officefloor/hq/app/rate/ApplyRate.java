package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the grade's rate from the from-date on the sheet, replacing the one it had for that from-date. */
public class ApplyRate {
    public void apply(@Val RateInput input, @Val Sheet sheet, RateRepository repository, Out<Rate> rateOut) {
        Rate rate = repository.findWithFrom(sheet.getId(), input.grade(), input.from())
                .orElseGet(() -> new Rate(sheet.getId(), input.grade(), input.pct(), input.from(), input.to()));
        rate.setPct(input.pct());
        rate.setToDate(input.to());
        rateOut.set(repository.saveAndFlush(rate));
    }
}
