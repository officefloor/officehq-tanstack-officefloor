package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") long planId, Plans plans,
            People people, Out<Plan> planOut, Out<Person> personOut) throws PersonNotFoundException {
        String sheet = sheetId.trim();
        Plan plan = plans.list(sheet).stream().filter(p -> p.id() == planId).findFirst().orElse(null);
        Person person = plan == null ? null : people.find(sheet, plan.personId());
        if (person == null) {
            throw new PersonNotFoundException("No plan " + planId + " on this sheet.");
        }
        planOut.set(plan);
        personOut.set(person);
    }
}
