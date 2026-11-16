package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim to the person; a claim given no date is dated the sheet's today. A claim dated inside a closed
 *  period, or after the person's retirement date, is refused, as is a
 *  negative amount that does not correct a SETTLED claim of the person (see Corrections). Its due-by is fixed here: its
 *  date plus the terms of the person's grade in force on that date. Its status log starts with its DRAFT line (see ClaimLog). */
public class AddClaim {
    public void add(@Val Person person, @Val ClaimValues values, SheetRepository sheets, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, ClaimLog log, Out<Claim> claim) throws InvalidClaimException {
        ClaimRules.Checked checked = ClaimRules.check(person, values, sheets, periods, claims);
        LocalDate date = checked.date();
        Claim added = claims.add(person.sheetId(), person.id(), values.amount(), date, values.note(), checked.corrects(),
                date.plusDays(settings.terms(person.sheetId(), person.gradeOn(date), date)));
        log.write(added, values.by());
        claim.set(added);
    }
}
