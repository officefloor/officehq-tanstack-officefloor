package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.DuplicatePersonContactException;
import net.officefloor.hq.app.person.DuplicatePersonNameException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonValues;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** The person on the sheet with the name (case ignored); when there is none, adds them (refused when the name is used on
 *  another sheet of the sheet's tree), writing PERSON_ADDED and the
 *  figures it moved just as adding a person does. Publishes the person and the figures before the claim is added. */
public class FindOrAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people,
            Rollup rollup, Audit audit, Out<Person> person, Out<RollupBefore> before) throws DuplicatePersonNameException,
            DuplicatePersonContactException {
        String sheet = sheetId.trim();
        Person found = people.findByName(sheet, values.name());
        if (found == null) {
            if (people.treeTaken(sheet, values.name())) {
                throw new DuplicatePersonNameException(values.name());
            }
            RollupBefore was = rollup.take(sheet);
            found = people.add(sheet, values.name(), values.grade(), values.terms(), values.contact());
            audit.record("PERSON_ADDED sheet=" + found.sheetId() + " by=" + values.by() + " name=" + found.name()
                    + " grade=" + found.grade());
            rollup.record(sheet, values.by(), was);
        }
        person.set(found);
        before.set(rollup.take(sheet));
    }
}
