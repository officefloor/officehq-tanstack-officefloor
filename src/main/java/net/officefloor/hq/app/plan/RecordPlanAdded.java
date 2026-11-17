package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Writes PLAN_ADDED to the audit file once the plan is stored; its descendant sheet at the end when it names one. */
public class RecordPlanAdded {
    public void record(@Val Person person, @Val PlanValues values, @Val Plan plan, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start()
                + (plan.day() != null ? " day=" + plan.day() : " every=" + plan.every())
                + " end=" + plan.end() + (plan.sheet() != null ? " sheet=" + plan.sheet() : ""));
    }
}
