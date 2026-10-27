package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class RememberTodayLeft {
    // Before the sheet's today is changed: the day being left, for the SNAPSHOT lines about it.
    public void remember(@PathVariable("sheetId") String sheetId, Sheets sheets, Out<TodayLeft> left) {
        left.set(new TodayLeft(sheets.today(sheetId.trim())));
    }
}
