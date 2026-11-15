package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Soft-closes a period: its due-total and each person's due-total for it are fixed now, as a close
 *  fixes them, but claims dated in it can still be added, edited, opened, settled and voided (a claim
 *  added then is a late entry). Refused when it is already closed or soft-closed, and for a role the
 *  permission matrix does not let close. */
public class SoftClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            ClaimRepository claims, ClaimLogRepository log, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, RoleRepository roles,
            Out<Period> period) throws InvalidPeriodException, PeriodClosedException, PeriodPermissionException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Permissions.allows(id, by, Permissions.CLOSE, roles, settings)) {
            throw new PeriodPermissionException("Your role may not close a period");
        }
        if (found.fixed()) {
            throw new PeriodClosedException("The period is already closed or soft-closed");
        }
        period.set(PeriodClosing.fixNow(found, by == null ? "" : by, true, periods, claims, log, people, rates, settings, sheets));
    }
}
