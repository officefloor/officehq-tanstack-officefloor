package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Changes the plan's amount from the sheet's today, for the claims it makes from then on (those
 *  already made keep theirs), notes the change and audits it. */
public class ChangePlanAmount {
    public void change(@Val Plan plan, @Val PlanAmount amount, @Val Actor actor, Plans plans, People people,
            Sheets sheets, Audit audit) {
        plans.changeAmount(plan.sheetId(), plan.id(), sheets.today(plan.sheetId()), amount.amount());
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + actor.user() + " name=" + person.getName()
                + " start=" + plan.start() + " amount=" + amount.amount().toPlainString());
    }
}
