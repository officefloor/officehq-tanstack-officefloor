package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A steward re-dates an OPEN or PENDING claim; not to a date after its person's retirement, nor a claim whose
 * due-by is inside a closed period. Its due-by stays as it was fixed and a figure a statement bound it to stays
 * bound; as its period goes by its due-by, it keeps its period and its number.
 */
public class ApplyRedate {
    public void apply(@Val ValidRedate request, @Val Claim claim, Settings settings, People people, Periods periods,
            Out<ClaimRedate> redate) throws ClaimStatusRefusedException {
        refuse(claim, request, settings, people, periods);
        redate.set(new ClaimRedate(claim.getDate()));
        // The claim's period goes by its due-by, which a re-dating leaves as it is: it keeps its period and number.
        claim.setDate(request.date());
    }

    /** Refuses a re-dating these rules do not allow; a bulk re-date applies the same rules to each claim. */
    static void refuse(Claim claim, ValidRedate request, Settings settings, People people, Periods periods)
            throws ClaimStatusRefusedException {
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
        if (periods.isClosed(sheetId, claim.getPeriodDate())) {
            throw new ClaimStatusRefusedException("No claim whose due-by is inside a closed period can be re-dated");
        }
    }
}
