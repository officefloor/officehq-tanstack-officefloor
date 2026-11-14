package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the plan just added to the person. */
public class AuditPlanAdded {
    public void audit(@Val Person person, @Val Plan plan, @RequestParam("user") String user, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name() + " amount="
                + plan.amount().setScale(2).toPlainString() + " start=" + plan.start() + (plan.day() != null ? " day=" + plan.day() : " every=" + plan.every())
                + " end=" + plan.end() + (plan.targetSheet() != null ? " sheet=" + plan.targetSheet() : ""));
    }
}
