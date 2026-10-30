package net.officefloor.hq.app.claim;

import java.util.Objects;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A steward re-dates an OPEN or PENDING claim; not to a date after its person's retirement, nor a claim dated
 * inside a closed period or to a date inside one. Its due-by stays as it was fixed and a figure a statement bound
 * it to stays bound. When per-period numbering is on and the new date is in another period, the claim takes the
 * next number of its new period's sequence (its old number is not used again).
 */
public class ApplyRedate {
    public void apply(@Val ValidRedate request, @Val Claim claim, Settings settings, People people, Periods periods,
            ClaimNumbering numbering, Out<ClaimRedate> redate) throws ClaimStatusRefusedException {
        String sheetId = claim.getSheetId();
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new ClaimStatusRefusedException("Only a steward can re-date a claim");
        }
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim can be re-dated");
        }
        Person person = people.find(sheetId, claim.getPersonId());
        if (person != null && !person.allowsClaimOn(request.date())) {
            throw new ClaimStatusRefusedException("No claim can be dated after the person's retirement date");
        }
        if (periods.isClosed(sheetId, claim.getDate()) || periods.isClosed(sheetId, request.date())) {
            throw new ClaimStatusRefusedException("No claim can be re-dated in or into a closed period");
        }
        redate.set(new ClaimRedate(claim.getDate()));
        boolean moved = !Objects.equals(periods.nameOn(sheetId, claim.getDate()), periods.nameOn(sheetId, request.date()));
        claim.setDate(request.date());
        if (moved && numbering.perPeriod(sheetId)) {
            claim.setNumber(numbering.next(sheetId, request.date()));
        }
    }
}
