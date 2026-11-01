package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Retires the person from the date: they count as retired at once, and their claims keep counting. */
public class RetirePerson {
    public void retire(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val LocalDate from, People people, Audit audit) {
        people.retire(sheet, person.id(), from);
        audit.record("PERSON_RETIRED sheet=" + sheet + " by=" + user + " name=" + person.name() + " from=" + from);
    }
}
