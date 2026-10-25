package net.officefloor.hq.app.history;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Snapshots the sheet as its today moves on to a later one, so a statement can be taken as of a date gone by. */
public class SnapshotSheet {
    public void snapshot(@Val SheetResponse sheet, @Val LocalDate today, SheetRepository sheets, SheetHistory history) {
        LocalDate was = sheets.today(sheet.id());
        if (today.isAfter(was)) {
            history.snapshot(sheet.id(), was, today);
        }
    }
}
