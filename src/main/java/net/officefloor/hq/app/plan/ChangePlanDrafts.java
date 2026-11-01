package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimLines;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Runs after the plan's change: each claim the plan made that is still a DRAFT takes the new amount,
 *  edited (keeping its date, note and the rest) and audited as the claim form edits it; one the form
 *  refuses keeps its amount. Claims in any other status keep theirs. */
public class ChangePlanDrafts {
    public void change(@Val Plan plan, @Val PlanAmount amount, @Val Actor actor, Plans plans, Claims claims,
            ClaimLines lines, People people, Sheets sheets, ClosedPeriods closedPeriods, Audit audit) {
        String claimSheet = plan.targetSheet() != null ? plan.targetSheet() : plan.sheetId();
        BigDecimal newAmount = amount.amount();
        for (long claimId : plans.madeClaims(plan.id())) {
            Claim claim = claims.find(claimSheet, claimId);
            if (claim == null || claim.getStatus() != ClaimStatus.DRAFT) continue;
            if (claim.getAmount().compareTo(newAmount) == 0) continue;
            ClaimValues values = new ClaimValues(newAmount, claim.getDate(), claim.getNote(), claim.getCorrects(),
                    actor.user(), claim.getRef(), claim.getTags());
            try {
                ClaimForm.edited(values, claim, sheets, closedPeriods, people, claims, lines, audit);
            } catch (Exception refused) {
                // the claim form refuses it: it keeps its amount
            }
        }
    }
}
