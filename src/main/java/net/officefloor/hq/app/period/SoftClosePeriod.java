package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Soft-close the named period, fixing its due-total, and each person's on the sheet, as a close does, while its
 *  claims can still be added, edited and change status. Refused when the user's role may not close (the sheet's
 *  permission matrix), there is no such period, or it is already closed or soft-closed. */
public class SoftClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            PeriodRepository periods, ClaimRepository claims, ClaimLogRepository logs, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, @Val PeriodCloseInput input, RoleRepository roles,
            Out<Period> closed) throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (!settings.permissions(id).allows("close", roles.of(id, input.user()))) {
            throw new InvalidPeriodException("Your role may not close a period on this sheet.");
        }
        Period period = periods.find(id, periodName);
        if (period == null) {
            throw new InvalidPeriodException("There is no period " + periodName.trim() + " on this sheet.");
        }
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        if (period.soft()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already soft-closed.");
        }
        closed.set(PeriodCloser.fix(id, period, true, periods, claims, logs, people, rates, settings, sheets));
    }
}
