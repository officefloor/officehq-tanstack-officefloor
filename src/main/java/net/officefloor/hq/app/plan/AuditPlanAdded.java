package net.officefloor.hq.app.plan;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records the plan added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditPlanAdded {
    public void audit(@Val Plan plan, @Val Person person, HttpServletRequest request, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + plan.sheetId() + " by=" + Audit.user(request) + " name=" + person.getName()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start() + (plan.day() != null ? " day=" + plan.day() : " every=" + plan.every())
                + " end=" + plan.end());
    }
}
