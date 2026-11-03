package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Resume a paused plan on the sheet's today, written as PLAN_RESUMED; resuming a running plan is refused. */
public class ResumePlan {
    public void resume(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            ServerHttpConnection connection, PlanRepository repository, SheetRepository sheets, Audit audit)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Plan plan = repository.lockedPlan(id, planId);
        if (!plan.paused()) {
            throw new BadRequestException("the plan is not paused");
        }
        repository.resume(plan.id(), sheets.today(id));
        audit.record("PLAN_RESUMED sheet=" + id + " by=" + by + " name=" + repository.personName(id, plan.personId())
                + " start=" + plan.start());
    }
}
