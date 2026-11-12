package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The person on the sheet with the quick add's name (ignoring case); when there is none, adds them
 * as the person form would, recording PERSON_ADDED and how it moved the figures. Runs only once the
 * claim has passed every check, so a refused quick add leaves no person behind.
 */
public class FindOrAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store,
            Audit audit, RollupService rollups, Out<Person> personOut) {
        String sheet = sheetId.trim();
        Person person = store.findByName(sheet, request.name());
        if (person == null) {
            Rollup before = rollups.snapshot(sheet);
            person = store.add(sheet, request.name(), request.grade());
            audit.record("PERSON_ADDED sheet=" + sheet + " by=" + request.user() + " name=" + person.getName()
                    + " grade=" + person.getGrade());
            rollups.record(sheet, request.user(), before);
        }
        personOut.set(person);
    }
}
