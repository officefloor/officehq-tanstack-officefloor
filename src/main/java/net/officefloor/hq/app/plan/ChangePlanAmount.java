package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.CheckClaimPeriod;
import net.officefloor.hq.app.claim.CheckClaimRetired;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLineStore;
import net.officefloor.hq.app.claim.ClaimOpening;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Changes a plan's amount on the sheet's today, recorded as PLAN_CHANGED sheet=<id> by=<user> name=<person>
 *  start=<plan start> amount=<new amount>. The claims it generates from then on are of the new amount; those
 *  already generated that are still DRAFT are edited to it, by plan date, as the claim form would edit them
 *  (recorded as CLAIM_EDITED; a draft such an edit would refuse keeps its amount); the others keep theirs.
 *  Publishes the plan as it now is. */
public class ChangePlanAmount {
    public void change(@Val PlanAmountRequest request, @Val Plan plan, PlanStore store, SheetStore sheets,
            PersonStore people, ClaimStore claims, ClaimLineStore lines, PeriodStore periods, RollupService rollups,
            Audit audit, Out<Plan> changed) {
        BigDecimal amount = new BigDecimal(request.amount());
        LocalDate today = sheets.today(plan.sheetId());
        store.changeAmount(plan.id(), today, amount);
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + request.user() + " name="
                + (person == null ? "" : person.getName()) + " start=" + plan.start() + " amount=" + request.amount());
        for (PlanStore.MadeClaim made : store.madeClaims(plan.id())) {
            Claim claim = claims.find(made.sheetId(), made.claimId());
            if (claim == null || claim.getStatus() != ClaimStatus.DRAFT) {
                continue;
            }
            ClaimInput input = new ClaimInput(amount, claim.getDate(), claim.getNote(), request.user(),
                    claim.getCorrects(), claim.getRef(), claim.getTags());
            try {
                new CheckClaimPeriod().check(claim, periods);
                new CheckClaimRetired().check(claim, input, people);
                ClaimOpening.editAmount(claim, input, claims, lines, people, rollups, audit);
            } catch (InvalidSheetException e) {
                // refused as the claim form would refuse it: the draft keeps its amount
            }
        }
        changed.set(store.find(plan.id()));
    }
}
