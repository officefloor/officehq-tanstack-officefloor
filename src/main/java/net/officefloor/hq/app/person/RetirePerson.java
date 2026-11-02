package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Retires the person from the date: they count as retired at once; from the day the sheet's today reaches the date,
 * their OPEN and PENDING claims, unless bound by a statement, count in no live total.
 */
public class RetirePerson {
    public void retire(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val LocalDate from, People people, Audit audit, Rollup rollup) {
        apply(sheet, user, person, from, people, audit, rollup);
    }

    /** Retires the person from the date, with its PERSON_RETIRED and rollup lines; the bulk retire uses it too. */
    static void apply(String sheet, String user, Person person, LocalDate from, People people, Audit audit,
            Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        people.retire(sheet, person.id(), from);
        audit.record("PERSON_RETIRED sheet=" + sheet + " by=" + user + " name=" + person.name() + " from=" + from);
        rollup.record(sheet, user, before);
    }
}
