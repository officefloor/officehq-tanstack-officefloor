package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateInput rate, RateStore store, SheetStore sheets) {
        store.set(sheetId.trim(), rate.grade(), rate.from(), rate.to(), rate.pct(), sheets.today(sheetId.trim()));
    }
}
