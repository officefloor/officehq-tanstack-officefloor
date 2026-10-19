package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The person the quick-add names (ignoring case). A name not on the sheet adds the person with grade
 * A, recorded as the person form records it — PERSON_ADDED then its ROLLUP lines — ahead of the claim.
 * Runs once every rule has passed, so a refused quick-add adds no one.
 */
public class ResolveQuickPerson {
    public void resolve(@PathVariable("sheetId") String sheetId, @Val QuickAdd quick,
            @RequestParam(value = "user", required = false) String user, PersonRepository repository,
            Rollup rollup, SheetClock clock, Audit audit, Out<Person> personOut) {
        Person person = repository.findByName(sheetId, quick.name());
        if (person == null) {
            Rollup.Snapshot before = rollup.take(sheetId);
            person = repository.create(sheetId, quick.name(), "A");
            audit.record("PERSON_ADDED sheet=" + sheetId + " by=" + user
                    + " name=" + person.getName() + " grade=" + person.gradeOn(clock.today(sheetId)));
            rollup.changes(sheetId, user, before, rollup.take(sheetId)).forEach(audit::record);
        }
        personOut.set(person);
    }
}
