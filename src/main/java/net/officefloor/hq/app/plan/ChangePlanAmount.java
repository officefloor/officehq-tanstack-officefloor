package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ChangePlanAmount {
    // Changes the plan's amount on the sheet's today (PLAN_CHANGED): the claims generated from then on are of the new
    // amount, those already generated keep theirs. An amount the claim form would refuse is refused, before anything is
    // stored or recorded.
    public void change(@RequestBody PlanAmountRequest body, @Val Plan plan, @Val Person person, Plans plans, Sheets sheets,
            ServerHttpConnection connection, Audit audit, Out<Plan> planOut) throws PlanRefusedException {
        ClaimInput input;
        try {
            input = ClaimFormRules.parse(new ClaimRequest(body == null ? "" : body.amount(), "", "", "", "", ""));
        } catch (InvalidClaimException e) {
            throw new PlanRefusedException(e.getMessage());
        }
        Plan changed = plans.changeAmount(plan, input.amount(), sheets.today(plan.sheetId()));
        audit.record(Audit.line("PLAN_CHANGED", plan.sheetId(), Audit.user(connection), "name=" + person.name(),
                "start=" + plan.start(), "amount=" + changed.amount().toPlainString()));
        planOut.set(changed);
    }
}
