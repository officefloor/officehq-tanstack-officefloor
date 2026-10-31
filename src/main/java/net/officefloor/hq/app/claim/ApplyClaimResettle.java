package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;

/** Re-settles a SETTLED claim: only a steward may, and never one due by in a closed period. Its due is
 *  fixed again at the figure it would have today as an OPEN claim bound by no statement: the rate of
 *  its person's grade in force on its date, plus its late charge today, at least the sheet's floor
 *  (n/a with no rate). It stays SETTLED, keeps its number and its binding. */
public class ApplyClaimResettle {
    public void apply(@Val ClaimResettle change, @Val Claim claim, People people, Figures figures,
            ClosedPeriods closedPeriods, Settings settings) throws ClaimStatusRefusedException, PeriodClosedException {
        if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can re-settle a claim");
        }
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new ClaimStatusRefusedException("Only a SETTLED claim can be re-settled");
        }
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        Claim open = new Claim(claim.getId(), claim.getSheetId(), claim.getPersonId(), claim.getAmount(),
                claim.getDate(), claim.getNote(), ClaimStatus.OPEN, claim.getNumber(), null, claim.getOpenedBy());
        open.setCorrects(claim.getCorrects());
        open.setDueBy(claim.getDueBy());
        String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
        change.setOld(claim.getDue());
        claim.setDue(figures.due(open, grade));
    }
}
