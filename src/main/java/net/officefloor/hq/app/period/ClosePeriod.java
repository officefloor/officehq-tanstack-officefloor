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

/** Closes a period (a soft-closed one too, fixing its figures afresh), fixing its due-total as it is now: the due, as the screen shows it now, of the claims
 *  whose date falls in it that counted by their status at the end of its last day as their status log
 *  records it (n/a ones skipped, and those the live rules leave out: deferred, or OPEN ones dated within
 *  a hold of their person, unless bound).
 *  Each person's due-total for it is fixed too, for everyone on the sheet. Refused when it is already closed, and for a role
 *  the permission matrix does not let close. */
public class ClosePeriod {
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
        if (found.closed()) {
            throw new PeriodClosedException("The period is already closed");
        }
        period.set(PeriodClosing.closeNow(found, by == null ? "" : by, periods, claims, log, people, rates, settings, sheets));
    }
}
