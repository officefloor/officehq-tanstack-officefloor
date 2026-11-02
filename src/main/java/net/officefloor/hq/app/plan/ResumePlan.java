package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Resumes the paused plan on the sheet's today; a plan not paused is refused. */
public class ResumePlan {
    public void resume(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Plan plan,
            Plans plans, People people, SheetClock clock, Audit audit) throws InvalidPlanException {
        if (!plans.resume(plan.id(), clock.today(sheet))) {
            throw new InvalidPlanException("The plan is not paused");
        }
        Person person = people.find(sheet, plan.person());
        audit.record("PLAN_RESUMED sheet=" + sheet + " by=" + user + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start());
    }
}
