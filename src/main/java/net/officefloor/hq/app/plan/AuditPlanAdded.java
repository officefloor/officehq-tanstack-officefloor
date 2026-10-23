package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record the stored plan in the audit file, as the screen shows it. */
public class AuditPlanAdded {
    public void audit(@Val PlanInput input, @Val Person person, @Val Plan plan, Audit audit) {
        PlanResponse shown = PlanResponse.from(plan, null);
        audit.record("PLAN_ADDED sheet=" + plan.sheetId() + " by=" + input.user() + " name=" + person.name()
                + " amount=" + shown.amount() + " start=" + shown.start() + (shown.day() != null ? " day=" + shown.day() : " every=" + shown.every())
                + " end=" + shown.end());
    }
}
