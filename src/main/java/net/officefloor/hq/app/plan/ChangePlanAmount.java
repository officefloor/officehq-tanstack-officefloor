package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Changes the plan's amount on the sheet's today, recording the change: the claims generated from then on take the
 *  new amount, those already generated keep theirs. */
public class ChangePlanAmount {
    public void change(@Val Sheet sheet, @Val Plan plan, @Val PlanAmountInput input, PlanRepository plans,
            PlanChangeRepository changes) {
        plan.setAmount(input.amount());
        plans.saveAndFlush(plan);
        changes.saveAndFlush(new PlanChange(plan.getId(), sheet.getToday(), input.amount()));
    }
}
