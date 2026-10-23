package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Bring the changed amount to the claims the plan generated that are still DRAFT, by plan date: each is edited as the
 *  claim form would edit it (its CLAIM_EDITED line and then how the sheet's figures moved over it), one the claim form
 *  would refuse is left as it is; claims in any other status keep their amount. A plan naming a descendant sheet edits
 *  its claims there, in that sheet's audit. */
public class EditPlanDrafts {
    public void edit(@Val PlanAmountInput input, @Val Plan loaded, PlanRepository plans, ClaimRepository claims,
            ClaimLineRepository lines, PersonRepository people, SheetRepository sheets, PeriodRepository periods,
            Rollups rollups, Audit audit) {
        String on = loaded.targetSheet() != null ? loaded.targetSheet() : loaded.sheetId();
        for (long id : plans.claimIds(loaded.id())) {
            Claim claim = claims.find(on, id);
            if (claim == null) {
                continue;
            }
            Rollup before = rollups.take(on);
            if (ClaimFormRules.editAmount(claim, input.user(), input.amount(), sheets, people, periods, claims, lines,
                    audit)) {
                rollups.record(on, input.user(), before);
            }
        }
    }
}
