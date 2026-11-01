package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SavePerson {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val PersonRequest request, People people, SheetClock clock, Audit audit, Rollup rollup)
            throws DuplicatePersonNameException, StalePersonException {
        // Saving over a version other than the one the editor loaded is refused, and writes nothing.
        long loaded = request.version() == null ? person.version() : request.version();
        if (loaded != person.version()) {
            throw new StalePersonException(person.id());
        }
        Rollup.Snapshot before = rollup.take(sheet);
        people.update(sheet, new Person(person.id(), request.name(), person.grade(), loaded, person.retiredFrom(),
                person.holds(), person.grades()));
        // The form's grade is recorded from its from-date (the sheet's today when blank), keeping the ones already
        // recorded; leaving the grade as the row showed it, with the from-date blank, records nothing.
        if (request.gradeFrom() != null || !request.grade().equals(person.grade())) {
            LocalDate from = request.gradeFrom() == null ? clock.today(sheet) : LocalDate.parse(request.gradeFrom());
            people.setGrade(person.id(), request.grade(), from);
        }
        Person saved = people.find(sheet, person.id());
        audit.record("PERSON_EDITED sheet=" + sheet + " by=" + user + " name=" + saved.name() + " grade="
                + saved.grade());
        rollup.record(sheet, user, before);
    }
}
