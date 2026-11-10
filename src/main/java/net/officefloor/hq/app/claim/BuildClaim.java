package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A new DRAFT claim, its due-by fixed now: its date plus its person's own terms, else the terms of their grade on that date, else the sheet's.
 * Dated inside a soft-closed period, it is a late entry.
 */
public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, SettingsRepository settings,
            PeriodRepository periods,
            Out<Claim> claimOut) {
        LocalDate date = input.date() == null ? sheet.today() : input.date();
        Claim claim = new Claim(null, sheet.id(), person.getId(), input.amount(), date, input.note(),
                ClaimStatus.DRAFT, null, null, null);
        claim.setCorrects(input.corrects());
        claim.setDueBy(date.plusDays(settings.find(sheet.id()).termsFor(person, date)));
        claim.setLateEntry(periods.softClosedOn(sheet.id(), date) != null);
        claimOut.set(claim);
    }
}
