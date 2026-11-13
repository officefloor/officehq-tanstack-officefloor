package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a plan: PLAN_ADDED sheet=<id> by=<user> name=<name> amount=<0.00> start=<date> every=<n> end=<date>, or
 *  for a monthly plan ... start=<date> day=<day> end=<date>, then sheet=<id> for a plan on a descendant sheet. */
public class AuditPlanAdded {
    public void audit(@Val PlanInput input, @Val Person person, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + person.getSheetId() + " by=" + input.user() + " name=" + person.getName()
                + " amount=" + input.amount().toPlainString() + " start=" + input.start() + (input.day() != null ? " day=" + input.day() : " every=" + input.every())
                + " end=" + input.end() + (input.sheet() != null ? " sheet=" + input.sheet() : ""));
    }
}
