package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Close the named period, fixing its due-total, and each person's on the sheet, as they are now: the figures (as shown on their rows) of the claims
 *  that count by the status they had at the end of its last day (see Dues.countsAtEnd) whose date falls in it, skipping n/a. Refused when the user's role may not close (the sheet's permission matrix), there is no such period, or it is already
 *  closed. */
public class ClosePeriod {
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
        closed.set(PeriodCloser.fix(id, period, periods, claims, logs, people, rates, settings, sheets));
    }
}
