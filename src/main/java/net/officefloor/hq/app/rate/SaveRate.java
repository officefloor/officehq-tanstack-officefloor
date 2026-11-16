package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the grade's rate from its from-date on the sheet, replacing any rate it had for that from-date. */
public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateValues values, RateRepository rates,
            Out<Rate> rate) {
        rate.set(rates.save(new Rate(sheetId.trim(), values.grade(), values.rate(), values.from())));
    }
}
