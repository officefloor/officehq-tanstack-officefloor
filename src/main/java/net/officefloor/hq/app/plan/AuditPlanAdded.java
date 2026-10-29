package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPlanAdded {
    // A plan naming a descendant sheet ends with sheet=<that sheet>.
    public void audit(@Val Plan plan, @Val Person person, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PLAN_ADDED", plan.sheetId(), Audit.user(connection), "name=" + person.name(),
                "amount=" + plan.amount().toPlainString(), "start=" + plan.start(), plan.monthly() ? "day=" + plan.day() : "every=" + plan.every(), "end=" + plan.end())
                + (plan.targetSheet().isEmpty() ? "" : " sheet=" + plan.targetSheet()));
    }
}
