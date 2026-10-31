package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Builds the new claim, its due-by fixed from the terms of its person's grade in force on its date, refusing one dated in a closed period or after its person's retirement date,
 *  and a correction that does not name a SETTLED claim of the same person. */
public class BuildClaim {
    public void build(@Val ClaimValues values, @Val Person person, Sheets sheets, ClosedPeriods closedPeriods,
            Claims claims, Settings settings, Out<Claim> claim) throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        LocalDate date = values.date() != null ? values.date() : sheets.today(person.getSheetId());
        closedPeriods.refuse(person.getSheetId(), date);
        person.refuseRetired(date);
        refuseUncorrected(values, person.getSheetId(), person.getId(), claims);
        Claim built = new Claim(null, person.getSheetId(), person.getId(), values.amount(), date, values.note(),
                ClaimStatus.DRAFT, null, null, null);
        built.setCorrects(values.corrects());
        built.setDueBy(date.plusDays(settings.terms(person.getSheetId(), person.gradeOn(date), date)));
        claim.set(built);
    }

    /** Refuses a correction whose number is not a SETTLED claim of the same person. */
    static void refuseUncorrected(ClaimValues values, String sheetId, long personId, Claims claims)
            throws InvalidSheetRequestException {
        if (values.corrects() != null && !claims.settled(sheetId, personId, values.corrects())) {
            throw new InvalidSheetRequestException("A correction must name a settled claim of the same person: " + values.corrects());
        }
    }
}
