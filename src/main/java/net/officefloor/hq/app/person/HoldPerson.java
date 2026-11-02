package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a hold to the person: their OPEN claims dated within it, unless bound by a statement, count in no live total. */
public class HoldPerson {
    public void hold(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val Hold hold, People people, Audit audit, Rollup rollup) {
        apply(sheet, user, person, hold, people, audit, rollup);
    }

    /** Adds the hold to the person, with its PERSON_HELD and rollup lines; the bulk hold uses it too. */
    static void apply(String sheet, String user, Person person, Hold hold, People people, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        people.hold(person.id(), hold);
        audit.record("PERSON_HELD sheet=" + sheet + " by=" + user + " name=" + person.name() + " from=" + hold.from());
        rollup.record(sheet, user, before);
    }
}
