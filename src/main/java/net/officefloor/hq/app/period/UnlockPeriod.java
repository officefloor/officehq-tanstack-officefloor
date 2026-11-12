package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Unlocks the period so it may be reopened again. Refused when the user is not a steward, there is no such
 *  period, or it is not locked. */
public class UnlockPeriod {
    public void unlock(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val CloseRequest request, PeriodStore store, RoleStore roles, Out<PeriodResponse> unlocked)
            throws InvalidSheetException {
        unlocked.set(PeriodLocking.set(sheetId.trim(), periodName.trim(), request.user(), false, store, roles));
    }
}
