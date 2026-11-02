package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Pauses the plan on the sheet's today; a plan already paused is refused. */
public class PausePlan {
    public void pause(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Plan plan,
            Plans plans, People people, SheetClock clock, Audit audit) throws InvalidPlanException {
        if (!plans.pause(plan.id(), clock.today(sheet))) {
            throw new InvalidPlanException("The plan is already paused");
        }
        Person person = people.find(sheet, plan.person());
        audit.record("PLAN_PAUSED sheet=" + sheet + " by=" + user + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start());
    }
}
