package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class PausePlan {
    // Pauses the plan on the sheet's today (PLAN_PAUSED), or resumes it on the sheet's today (PLAN_RESUMED). Pausing a
    // paused plan or resuming a running one is refused, before anything is stored or recorded.
    public void pause(@RequestBody PlanPauseRequest body, @Val Plan plan, @Val Person person, Plans plans, Sheets sheets,
            ServerHttpConnection connection, Audit audit) throws PlanRefusedException {
        boolean pause = body != null && body.paused();
        if (pause == plans.paused(plan.id())) {
            throw new PlanRefusedException(pause ? "The plan is already paused." : "The plan is not paused.");
        }
        if (pause) {
            plans.pause(plan.id(), sheets.today(plan.sheetId()));
        } else {
            plans.resume(plan.id(), sheets.today(plan.sheetId()));
        }
        audit.record(Audit.line(pause ? "PLAN_PAUSED" : "PLAN_RESUMED", plan.sheetId(), Audit.user(connection),
                "name=" + person.name(), "start=" + plan.start()));
    }
}
