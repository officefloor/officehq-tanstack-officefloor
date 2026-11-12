package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Locks the period so it cannot be reopened. Refused when the user is not a steward, there is no such period, or
 *  it is already locked. */
public class LockPeriod {
    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val CloseRequest request, PeriodStore store, RoleStore roles, Out<PeriodResponse> locked)
            throws InvalidSheetException {
        locked.set(PeriodLocking.set(sheetId.trim(), periodName.trim(), request.user(), true, store, roles));
    }
}
