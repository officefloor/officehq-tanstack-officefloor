package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a plan: PLAN_ADDED sheet=<id> by=<user> name=<name> amount=<0.00> start=<date> every=<n> end=<date>. */
public class AuditPlanAdded {
    public void audit(@Val PlanInput input, @Val Person person, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + person.getSheetId() + " by=" + input.user() + " name=" + person.getName()
                + " amount=" + input.amount().toPlainString() + " start=" + input.start() + " every=" + input.every()
                + " end=" + input.end());
    }
}
