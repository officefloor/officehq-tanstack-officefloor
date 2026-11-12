package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Soft-closes the period, fixing its due-total and each person's due-total in it as they are now, as a close
 *  does, while claims due by a date inside it may still be added, edited and moved on. Refused as a close is: when the
 *  sheet's permission matrix does not allow the user's role to close, there is no such period, or it is
 *  already closed or soft-closed. */
public class SoftClosePeriod {
    public void softClose(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val CloseRequest request, PeriodStore store, RoleStore roles, SettingStore settings, RollupService rollups,
            Out<PeriodResponse> closed) throws InvalidSheetException {
        String sheet = sheetId.trim();
        Role role = roles.find(sheet, request.user());
        if (!settings.allows(sheet, "close", role)) {
            throw new InvalidSheetException("A " + role.shown() + " may not close a period");
        }
        PeriodResponse period = store.find(sheet, periodName.trim());
        if (period == null) {
            throw new InvalidSheetException("No such period: " + periodName.trim());
        }
        if (period.closed() || period.soft()) {
            throw new InvalidSheetException("The period " + period.name() + " is already closed");
        }
        closed.set(PeriodClosing.fixSoft(sheet, period, store, rollups));
    }
}
