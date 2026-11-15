package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Changes the plan's amount from the sheet's today (PLAN_CHANGED): the claims it makes from then on are of
 *  the new amount, the claims it has made keep theirs. */
public class ChangePlanAmount {
    public void change(@RequestParam(name = "by", required = false) String by, @Val ValidPlanAmount request,
            @Val Plan plan, @Val Person person, PlanRepository plans, SheetRepository sheets, Audit audit) {
        BigDecimal amount = request.amount();
        plans.changeAmount(plan.id(), sheets.today(plan.sheetId()), amount);
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " start=" + plan.start() + " amount=" + amount.toPlainString());
    }
}
