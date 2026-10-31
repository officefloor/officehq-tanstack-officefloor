package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Builds every imported person first, as the person form would, so one refused line refuses the
 *  whole import before anything is saved; then saves each in order, audited PERSON_ADDED and then
 *  the figures it moved, as the form's add does. */
public class AddImportedPeople {
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, People people,
            Rollup rollup, Audit audit, Out<ImportPeopleResult> result)
            throws InvalidSheetRequestException, PersonConflictException {
        List<Person> built = new ArrayList<>();
        for (PersonRequest line : imported.lines()) {
            built.add(BuildPerson.built(sheetId, line, people));
        }
        SavePerson save = new SavePerson();
        RecordPersonAdded record = new RecordPersonAdded();
        for (int i = 0; i < built.size(); i++) {
            Snapshot before = rollup.snapshot(sheetId);
            save.save(built.get(i), people);
            record.record(imported.lines().get(i), built.get(i), audit);
            rollup.record(sheetId, before, imported.user());
        }
        result.set(new ImportPeopleResult(built.size()));
    }
}
