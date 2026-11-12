package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateInput rate, RateStore store) {
        store.set(sheetId.trim(), rate.grade(), rate.from(), rate.pct());
    }
}
