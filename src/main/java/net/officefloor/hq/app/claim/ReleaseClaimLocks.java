package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** A change of the sheet's today releases every claim's lock (closes every open claim editor). */
public class ReleaseClaimLocks {
    public void release(@Val Sheet sheet, @Val LocalDate today, ClaimRepository claims) {
        if (!today.equals(sheet.today())) {
            claims.unlockAll(sheet.id());
        }
    }
}
