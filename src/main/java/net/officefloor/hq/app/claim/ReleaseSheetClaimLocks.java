package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** A change of the sheet's today drops every claim lock on it. Runs before the new today is stored. */
public class ReleaseSheetClaimLocks {
    public void release(@Val SheetResponse sheet, @Val LocalDate today, SheetRepository sheets, ClaimLockRepository locks) {
        if (!today.equals(sheets.today(sheet.id()))) {
            locks.releaseAll(sheet.id());
        }
    }
}
