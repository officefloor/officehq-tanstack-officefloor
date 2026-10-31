package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Builds the new claim, its due-by fixed from its person's own terms, else those of their grade in force on its date, a late entry when a soft-closed period contains its due-by, refusing one due by in a closed period or after its person's retirement date,
 *  and a correction that does not name a SETTLED claim of the same person. */
public class BuildClaim {
    public void build(@Val ClaimValues values, @Val Person person, Sheets sheets, ClosedPeriods closedPeriods,
            Claims claims, Settings settings, Holidays holidays, Out<Claim> claim) throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        claim.set(built(values, person, sheets, closedPeriods, claims, settings, holidays));
    }

    /** The new DRAFT claim for these values and person, or refused by the claim form's rules. */
    static Claim built(ClaimValues values, Person person, Sheets sheets, ClosedPeriods closedPeriods,
            Claims claims, Settings settings, Holidays holidays) throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        LocalDate date = values.date() != null ? values.date() : settings.claimDate(person.getSheetId());
        LocalDate dueBy = DueBy.of(person, date, settings, holidays);
        closedPeriods.refuse(person.getSheetId(), dueBy);
        person.refuseRetired(date);
        refuseUncorrected(values, person.getSheetId(), person.getId(), claims);
        Claim built = new Claim(null, person.getSheetId(), person.getId(), values.amount(), date, values.note(),
                ClaimStatus.DRAFT, null, null, null);
        built.setCorrects(values.corrects());
        built.setDueBy(dueBy);
        built.setLateEntry(closedPeriods.late(person.getSheetId(), dueBy));
        return built;
    }

    /** Refuses a correction whose number is not a SETTLED claim of the same person. */
    static void refuseUncorrected(ClaimValues values, String sheetId, long personId, Claims claims)
            throws InvalidSheetRequestException {
        if (values.corrects() != null && !claims.settled(sheetId, personId, values.corrects())) {
            throw new InvalidSheetRequestException("A correction must name a settled claim of the same person: " + values.corrects());
        }
    }
}
