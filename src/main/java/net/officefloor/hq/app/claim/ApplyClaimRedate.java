package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** A steward re-dates an OPEN or PENDING claim. Its due-by stays as it was fixed and a figure a
 *  statement bound stays bound; so does its period (the one containing its due-by) and its number.
 *  Refused: anyone but a steward, a claim not OPEN or PENDING, a claim due by in a closed period,
 *  or a new date after its person's retirement. */
public class ApplyClaimRedate {
    public void apply(@Val ClaimRedate change, @Val Claim claim, People people, Settings settings, Sheets sheets,
            ClosedPeriods closedPeriods)
            throws ClaimStatusRefusedException, PeriodClosedException, PersonRetiredException {
        String sheetId = claim.getSheetId();
        if (sheets.archived(sheetId) || settings.role(sheetId, change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can re-date a claim");
        }
        if (!claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be re-dated");
        }
        closedPeriods.refuse(sheetId, claim.getDueBy());
        people.find(sheetId, claim.getPersonId()).refuseRetired(change.date());
        change.redated(claim.getDate());
        claim.setDate(change.date());
    }
}
