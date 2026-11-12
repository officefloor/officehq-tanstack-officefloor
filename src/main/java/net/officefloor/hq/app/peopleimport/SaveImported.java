package net.officefloor.hq.app.peopleimport;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds each imported person in order as the person form would, recorded as PERSON_ADDED (and its ROLLUP). */
public class SaveImported {
    public void save(@PathVariable("sheetId") String sheetId, @Val ImportInput imported, PersonStore store,
            RollupService rollups, Audit audit, Out<ImportResult> result) {
        String sheet = sheetId.trim();
        for (PersonRequest request : imported.people()) {
            Rollup before = rollups.snapshot(sheet);
            Person person = store.add(sheet, request.name(), request.grade(),
                    request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact());
            audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                    + person.getName() + " grade=" + request.grade()
                    + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                    + (person.getContact() == null ? "" : " contact=" + person.getContact()));
            rollups.record(person.getSheetId(), request.user(), before);
        }
        result.set(new ImportResult(imported.people().size()));
    }
}
