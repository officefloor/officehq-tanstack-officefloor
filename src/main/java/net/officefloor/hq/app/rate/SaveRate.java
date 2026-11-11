package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveRate {
    public void save(@Val RateInput input, @Val Sheet sheet, RateRepository repository,
            Out<Rate> rateOut) {
        Rate rate = new Rate(sheet.id(), input.grade(), input.pct(), input.from(), input.to());
        rateOut.set(repository.save(rate));
    }
}
