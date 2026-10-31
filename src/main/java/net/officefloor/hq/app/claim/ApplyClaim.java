package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Edits the claim, refusing when it is no longer a DRAFT, or is dated in a closed period, or would be moved into one,
 *  or would be dated after its person's retirement date. */
public class ApplyClaim {
    public void apply(@Val ClaimValues values, @Val Claim claim, Sheets sheets, ClosedPeriods closedPeriods,
            People people) throws ClaimEditRefusedException, PeriodClosedException, PersonRetiredException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimEditRefusedException(claim.getStatus());
        }
        LocalDate date = values.date() != null ? values.date() : sheets.today(claim.getSheetId());
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        closedPeriods.refuse(claim.getSheetId(), date);
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null) person.refuseRetired(date);
        claim.setAmount(values.amount());
        claim.setDate(date);
        claim.setNote(values.note());
    }
}
