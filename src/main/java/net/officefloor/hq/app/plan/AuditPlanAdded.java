package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the plan, once it is added. */
public class AuditPlanAdded {
    public void audit(@Val Plan plan, @Val Person person,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + plan.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " amount=" + plan.getAmount().setScale(2).toPlainString()
                + " start=" + plan.getStart() + " every=" + plan.getEvery() + " end=" + plan.getEnd());
    }
}
