package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Unretires a retired person on the sheet's today: they are no longer retired, their retirement kept as a past one,
 * from its retired-from date to today. Anyone not retired is refused.
 */
public class UnretirePerson {
    public void unretire(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            People people, SheetClock clock, Audit audit, Rollup rollup) throws InvalidPersonException {
        if (person.retiredFrom() == null) {
            throw new InvalidPersonException("Only a retired person can be unretired");
        }
        LocalDate on = clock.today(sheet);
        Rollup.Snapshot before = rollup.take(sheet);
        people.unretire(sheet, person.id(), LocalDate.parse(person.retiredFrom()), on);
        audit.record("PERSON_UNRETIRED sheet=" + sheet + " by=" + user + " name=" + person.name() + " on=" + on);
        rollup.record(sheet, user, before);
    }
}
