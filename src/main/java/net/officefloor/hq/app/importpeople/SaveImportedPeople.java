package net.officefloor.hq.app.importpeople;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.AuditPersonAdded;
import net.officefloor.hq.app.person.AuditPersonEdited;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.StalePersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Saves each imported person, in order, as the person form does: a new one added and recorded as PERSON_ADDED, an
 * existing one updated and recorded as PERSON_EDITED, each followed by the rollup lines that save moved.
 */
public class SaveImportedPeople {
    public void save(@Val ImportedPersons imported, @RequestParam("user") String user, PersonRepository repository,
            RollupService rollups, Audit audit, Out<ImportResult> result) throws StalePersonException {
        int added = 0;
        int updated = 0;
        for (ImportedPersons.Entry entry : imported.people()) {
            Person person = entry.person();
            Rollup before = rollups.compute(person.sheetId());
            Person stored = repository.save(person);
            if (entry.update()) {
                if (stored == null) {
                    throw new StalePersonException(person.name());
                }
                new AuditPersonEdited().audit(stored, entry.termsChange(), user, audit);
                updated++;
            } else {
                new AuditPersonAdded().audit(stored, user, audit);
                added++;
            }
            RollupLines.record(before, rollups.compute(person.sheetId()), person.sheetId(), user, audit);
        }
        result.set(new ImportResult(added, updated));
    }
}
