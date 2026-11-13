package net.officefloor.hq.app.importpeople;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.AuditPersonAdded;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds each imported person, in order, as the person form's add does: saved, recorded as PERSON_ADDED and followed by
 * the rollup lines that add moved.
 */
public class SaveImportedPeople {
    public void save(@Val ImportedPersons imported, @RequestParam("user") String user, PersonRepository repository,
            RollupService rollups, Audit audit, Out<ImportResult> result) {
        int added = 0;
        for (Person person : imported.people()) {
            Rollup before = rollups.compute(person.sheetId());
            Person stored = repository.save(person);
            new AuditPersonAdded().audit(stored, user, audit);
            RollupLines.record(before, rollups.compute(person.sheetId()), person.sheetId(), user, audit);
            added++;
        }
        result.set(new ImportResult(added));
    }
}
