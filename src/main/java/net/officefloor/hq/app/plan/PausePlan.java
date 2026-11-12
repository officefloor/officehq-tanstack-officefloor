package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Pauses a running plan, or resumes a paused one, on the sheet's today, recorded as PLAN_PAUSED (or
 *  PLAN_RESUMED) sheet=<id> by=<user> name=<person> start=<plan start>. Pausing a paused plan or resuming a
 *  running one is refused. Publishes the plan as it now is. */
public class PausePlan {
    public void pause(@Val PlanPauseRequest request, @Val Plan plan, PlanStore store, SheetStore sheets,
            PersonStore people, Audit audit, Out<Plan> changed) throws InvalidSheetException {
        boolean on = request.on();
        if (on && plan.paused()) {
            throw new InvalidSheetException("The plan is already paused");
        }
        if (!on && !plan.paused()) {
            throw new InvalidSheetException("The plan is not paused");
        }
        LocalDate today = sheets.today(plan.sheetId());
        if (on) {
            store.pause(plan.id(), today);
        } else {
            store.resume(plan.id(), today);
        }
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record((on ? "PLAN_PAUSED" : "PLAN_RESUMED") + " sheet=" + plan.sheetId() + " by=" + request.user()
                + " name=" + (person == null ? "" : person.getName()) + " start=" + plan.start());
        changed.set(store.find(plan.id()));
    }
}
