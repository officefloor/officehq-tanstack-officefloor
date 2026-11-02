package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.DuplicatePersonNameException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The person on the sheet with the typed name (ignoring case), else a new one of the sheet's default grade
 * (grade A when that is retired). */
public class ResolveQuickAddPerson {
    public void resolve(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val QuickAddRequest request, People people, Settings settings, Audit audit, Rollup rollup, Out<Person> person)
            throws DuplicatePersonNameException {
        Person found = people.findByName(sheet, request.name());
        if (found != null) {
            person.set(found);
            return;
        }
        Rollup.Snapshot before = rollup.take(sheet);
        // A retired default grade cannot be chosen for the new person: they take grade A instead.
        String grade = settings.defaultGrade(sheet);
        if (people.gradeRetired(sheet, grade)) {
            grade = "A";
        }
        Person added = people.add(sheet, request.name(), grade);
        person.set(added);
        audit.record("PERSON_ADDED sheet=" + sheet + " by=" + user + " name=" + added.name() + " grade="
                + added.grade());
        rollup.record(sheet, user, before);
    }
}
