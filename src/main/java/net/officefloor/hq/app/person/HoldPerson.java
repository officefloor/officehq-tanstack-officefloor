package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Puts the person on hold, or takes them off it when held: while held, their OPEN claims count in no live total. */
public class HoldPerson {
    public void hold(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            People people, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        boolean held = !person.held();
        people.hold(sheet, person.id(), held);
        audit.record((held ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + sheet + " by=" + user + " name="
                + person.name());
        rollup.record(sheet, user, before);
    }
}
