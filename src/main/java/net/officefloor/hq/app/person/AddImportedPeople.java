package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the checked people in the order pasted, each as the person form adds one: stored, written as PERSON_ADDED
 *  (see RecordPersonAdded), then the figures it moved (see Rollup). */
public class AddImportedPeople {
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, PersonRepository people,
            Rollup rollup, Audit audit, Out<ImportPeopleResult> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        String sheet = sheetId.trim();
        for (PersonValues values : imported.people()) {
            RollupBefore before = rollup.take(sheet);
            Person person = people.add(sheet, values.name(), values.grade(), values.terms(), values.contact());
            new RecordPersonAdded().record(person, values, before, rollup, audit);
        }
        result.set(new ImportPeopleResult(imported.people().size()));
    }
}
