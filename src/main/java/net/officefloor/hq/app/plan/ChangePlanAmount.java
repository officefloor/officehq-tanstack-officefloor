package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Changes the plan's amount from the sheet's today: an amount the plan form takes (see ValidatePlan), any other
 *  refused. The claims generated from then on take the new amount; those already made keep theirs. Writes
 *  PLAN_CHANGED. */
public class ChangePlanAmount {
    public void change(@PathVariable("sheetId") String sheetId, @RequestBody PlanAmountRequest request, @Val Plan plan,
            PlanRepository plans, PersonRepository people, Audit audit) throws InvalidPlanException {
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
        audit.record("PLAN_CHANGED sheet=" + sheet + " by=" + claim.by() + " name="
                + (person == null ? "" : person.name()) + " start=" + plan.start() + " amount="
                + change.amount().toPlainString());
    }
}
