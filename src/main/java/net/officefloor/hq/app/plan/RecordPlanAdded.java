package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the plan is saved, so a refused one writes no audit line. */
public class RecordPlanAdded {
    public void record(@Val PlanValues values, @Val Person person, @Val Plan plan, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + plan.sheetId() + " by=" + values.user() + " name=" + person.getName()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start() + (plan.every() != null ? " every=" + plan.every() : " day=" + plan.day())
                + " end=" + plan.end());
    }
}
