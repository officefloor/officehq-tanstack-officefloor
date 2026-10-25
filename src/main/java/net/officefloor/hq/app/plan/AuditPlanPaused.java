package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the plan, once it is paused. */
public class AuditPlanPaused {
    public void audit(@Val Plan plan, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String name = people.findByIdAndSheetId(plan.getPersonId(), plan.getSheetId()).map(p -> p.getName()).orElse("");
        audit.record("PLAN_PAUSED sheet=" + plan.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + name + " start=" + plan.getStart());
    }
}
