package net.officefloor.hq.app.asof;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Keeps the sheet as it stands at the end of its today, before its today moves on. */
public class RecordSheetDay {
    public void record(@Val Sheet sheet, @Val LocalDate today, SheetDayRepository days) {
        days.record(sheet.id(), sheet.today(), today);
    }
}
