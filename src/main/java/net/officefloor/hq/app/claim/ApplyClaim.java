package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Edits the claim and releases its editor's lock, refusing when another user's editor locks it, or it is no longer a DRAFT, or is due by in a closed period (its due-by, fixed when it was added, stays as it is),
 *  or would be dated after its person's retirement date, or would be a correction of no SETTLED claim of its person. */
public class ApplyClaim {
    public void apply(@Val ClaimValues values, @Val Claim claim, Sheets sheets, ClosedPeriods closedPeriods,
            People people, Claims claims)
            throws ClaimEditRefusedException, ClaimLockedException, PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        ApplyClaimLock.refuseLocked(claim, values.user());
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimEditRefusedException(claim.getStatus());
        }
        LocalDate date = values.date() != null ? values.date() : sheets.today(claim.getSheetId());
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null) person.refuseRetired(date);
        BuildClaim.refuseUncorrected(values, claim.getSheetId(), claim.getPersonId(), claims);
        BuildClaim.refuseUsedRef(values.ref(), claim.getSheetId(), claim.getId(), claims);
        claim.setAmount(values.amount());
        claim.setDate(date);
        claim.setNote(values.note());
        claim.setCorrects(values.corrects());
        claim.setRef(values.ref());
        claim.setTags(values.tags());
        claim.setLockedBy(null);
    }
}
