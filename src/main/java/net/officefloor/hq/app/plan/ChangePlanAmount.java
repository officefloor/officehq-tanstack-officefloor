package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimLocks;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ChangePlanAmount {
    // Changes the plan's amount on the sheet's today (PLAN_CHANGED): the claims generated from then on are of the new
    // amount, and those it generated that are still DRAFT take it too, in the order of their dates, each as the claim
    // editor would save it (CLAIM_EDITED; one the editor would refuse keeps its amount); claims in any other status keep
    // theirs. An amount the claim form would refuse is refused, before anything is
    // stored or recorded.
    public void change(@RequestBody PlanAmountRequest body, @Val Plan plan, @Val Person person, Plans plans, Sheets sheets,
            Claims claims, ClaimLocks locks, Periods periods, People people, ServerHttpConnection connection,
            Audit audit, Out<Plan> planOut) throws PlanRefusedException {
        ClaimInput input;
        try {
            input = ClaimFormRules.parse(new ClaimRequest(body == null ? "" : body.amount(), "", "", "", "", ""));
        } catch (InvalidClaimException e) {
            throw new PlanRefusedException(e.getMessage());
        }
        Plan changed = plans.changeAmount(plan, input.amount(), sheets.today(plan.sheetId()));
        audit.record(Audit.line("PLAN_CHANGED", plan.sheetId(), Audit.user(connection), "name=" + person.name(),
                "start=" + plan.start(), "amount=" + changed.amount().toPlainString()));
        String user = Audit.user(connection);
        String on = plan.claimSheet();
        for (long claimId : plans.madeClaims(plan.id())) {
            Claim claim = claims.find(on, claimId);
            if (!ClaimFormRules.editableAmount(claim, changed.amount(), user, claims, locks, periods, people)) {
                continue;
            }
            Claim edited = claim.with(new ClaimInput(changed.amount(), claim.date(), claim.note(), claim.corrects(),
                    claim.ref(), claim.tags()), claim.date());
            claims.save(edited, user.trim());
            Person owner = people.find(on, claim.personId());
            ClaimResponse shown = ClaimResponse.from(edited);
            audit.record(Audit.line("CLAIM_EDITED", on, user, "person=" + (owner == null ? "" : owner.name()),
                    "amount=" + shown.amount(), "date=" + shown.date()));
        }
        planOut.set(changed);
    }
}
