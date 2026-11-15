package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a plan that was added is recorded (PLAN_ADDED). */
public class RecordPlanAdded {
    public void record(@Val Plan plan, @Val Person person, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + plan.sheetId() + " by=" + plan.by() + " name=" + person.getName()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start() + " every=" + plan.every()
                + " end=" + plan.end());
    }
}
