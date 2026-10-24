package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the grade's rate on the sheet, replacing the one it had. */
public class ApplyRate {
    public void apply(@Val RateInput input, @Val Sheet sheet, RateRepository repository, Out<Rate> rateOut) {
        Rate rate = repository.findBySheetIdAndGrade(sheet.getId(), input.grade())
                .orElseGet(() -> new Rate(sheet.getId(), input.grade(), input.pct()));
        rate.setPct(input.pct());
        rateOut.set(repository.saveAndFlush(rate));
    }
}
