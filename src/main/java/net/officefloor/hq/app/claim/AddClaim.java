package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim to the person; a claim given no date is dated the sheet's entry date, else its today. A claim whose due-by falls inside
 *  a closed period, or dated after the person's retirement date, is refused, as is a
 *  negative amount that does not correct a SETTLED claim of the person (see Corrections). Its due-by is fixed here: its
 *  terms-th business day after its date (see ClaimRules.dueBy). A claim whose due-by falls inside a soft-closed period is
 *  marked a late entry. Its status log starts with its DRAFT line (see ClaimLog). */
public class AddClaim {
    public void add(@Val Person person, @Val ClaimValues values, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, HolidayRepository holidays, ClaimLog log, Out<Claim> claim) throws InvalidClaimException {
        ClaimRules.Checked checked = ClaimRules.check(person, values, settings, periods, claims, holidays);
        LocalDate date = checked.date();
        Claim added = claims.add(person.sheetId(), person.id(), values.amount(), date, values.note(), checked.corrects(),
                checked.dueBy());
        log.write(added, values.by());
        if (periods.softClosedOn(added.sheetId(), checked.dueBy())) {
            claims.markLateEntry(added);
        }
        claim.set(added);
    }
}
