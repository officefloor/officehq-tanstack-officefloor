package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Changes the plan's amount on the sheet's today (PLAN_CHANGED): the claims it makes from then on are of the new
 * amount, those already made keep theirs. Refused (400) for an amount the plan form would refuse.
 */
public class ChangePlanAmount {
    public void change(@Val Plan plan, @Val PlanAmountRequest request, @RequestParam("user") String user,
            PlanRepository plans, SheetRepository sheets, PersonRepository people, Audit audit, Out<Plans> saved)
            throws InvalidPlanException {
        BigDecimal amount;
        try {
            amount = ClaimForm.check(new ClaimRequest(request.amount(), "", "", "", "", "")).amount();
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        plans.changeAmount(plan, sheets.today(plan.sheetId()), amount);
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + user + " name=" + person.name() + " start="
                + plan.start() + " amount=" + amount.setScale(2).toPlainString());
        saved.set(new Plans(java.util.List.of(plans.find(plan.sheetId(), plan.id()))));
    }
}
