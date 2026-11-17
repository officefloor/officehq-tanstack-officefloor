package net.officefloor.hq.app.plan;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ApplyClaim;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Changes the plan's amount from the sheet's today: an amount the plan form takes (see ValidatePlan), any other
 *  refused. The claims generated from then on take the new amount; of those already made, each still a DRAFT (on
 *  whichever sheet the plan made it) is edited to the new amount as the claim's editor would edit it, keeping its date,
 *  note, reference and tags (see ApplyClaim, SaveClaim), writing CLAIM_EDITED and then the figures it moved (see Rollup)
 *  when its amount changes; claims in any other status keep theirs. An edit the editor would refuse refuses the whole
 *  change. Writes PLAN_CHANGED first; nothing is written to the audit file until every edit has gone through. */
public class ChangePlanAmount {
    public void change(@PathVariable("sheetId") String sheetId, @RequestBody PlanAmountRequest request, @Val Plan plan,
            PlanRepository plans, PersonRepository people, ClaimRepository claims, SheetRepository sheets,
            PeriodRepository periods, Rollup rollup, Audit audit) throws InvalidPlanException {
        if (request == null) {
            throw new InvalidPlanException("An amount is required");
        }
        String sheet = sheetId.trim();
        ClaimValues claim;
        try {
            claim = ClaimValues.from(new ClaimRequest(request.amount(), null, "", null, request.by(), null, null));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        if (claim.amount().signum() < 0) {
            throw new InvalidPlanException("A plan's amount must be 0.00 or more");
        }
        Person person = people.find(sheet, plan.personId());
        PlanChange change = plans.change(sheet, plan.id(), claim.amount());
        List<String> lines = new ArrayList<>();
        lines.add("PLAN_CHANGED sheet=" + sheet + " by=" + claim.by() + " name="
                + (person == null ? "" : person.name()) + " start=" + plan.start() + " amount="
                + change.amount().toPlainString());
        String on = plan.sheet() == null ? sheet : plan.sheet();
        for (long id : plans.claimIds(plan.id())) {
            Claim draft = claims.find(on, id);
            if (draft == null || draft.status() != ClaimStatus.DRAFT || draft.amount().compareTo(change.amount()) == 0) {
                continue;
            }
            RollupBefore before = rollup.take(on);
            ClaimValues values = new ClaimValues(change.amount(), draft.date(), draft.note(), draft.corrects(), claim.by(),
                    draft.ref(), draft.tags());
            Claim[] edited = new Claim[1];
            try {
                new ApplyClaim().apply(draft, values, sheets, periods, people, claims, (c) -> edited[0] = c);
                if (!claims.update(edited[0], claim.by(), sheets.today(on))) {
                    throw new InvalidClaimException("Only a DRAFT claim no one else is editing can be edited");
                }
            } catch (InvalidClaimException e) {
                throw new InvalidPlanException(e.getMessage());
            }
            Person owner = people.find(on, draft.personId());
            lines.add("CLAIM_EDITED sheet=" + on + " by=" + claim.by() + " person="
                    + (owner == null ? "" : owner.name()) + " amount=" + edited[0].amount().toPlainString()
                    + " date=" + edited[0].date());
            lines.addAll(rollup.lines(on, claim.by(), before));
        }
        lines.forEach(audit::record);
    }
}
