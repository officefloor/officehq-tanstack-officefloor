package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Builds the new claim, refusing one dated in a closed period or after its person's retirement date. */
public class BuildClaim {
    public void build(@Val ClaimValues values, @Val Person person, Sheets sheets, ClosedPeriods closedPeriods,
            Out<Claim> claim) throws PeriodClosedException, PersonRetiredException {
        LocalDate date = values.date() != null ? values.date() : sheets.today(person.getSheetId());
        closedPeriods.refuse(person.getSheetId(), date);
        person.refuseRetired(date);
        claim.set(new Claim(null, person.getSheetId(), person.getId(), values.amount(), date, values.note(),
                ClaimStatus.DRAFT, null, null, null));
    }
}
