package net.officefloor.hq.app.peopleimport;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.StalePersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Applies each imported line in order. A line naming a person on the sheet updates them as the person form
 *  would — the grade recorded from the sheet's today, their terms and contact set when the line gives them —
 *  recorded as PERSON_EDITED (and its ROLLUP); any other line adds a person as the form would, recorded as
 *  PERSON_ADDED (and its ROLLUP). */
public class SaveImported {
    public void save(@PathVariable("sheetId") String sheetId, @Val ImportInput imported, PersonStore store,
            SheetStore sheets, RollupService rollups, Audit audit, Out<ImportResult> result)
            throws StalePersonException {
        String sheet = sheetId.trim();
        int added = 0;
        int updated = 0;
        for (ImportInput.Line line : imported.lines()) {
            PersonRequest request = line.request();
            Rollup before = rollups.snapshot(sheet);
            if (line.existing() != null) {
                Person person = line.existing();
                if (request.terms() != null) {
                    person.setTerms(Integer.valueOf(request.terms()));
                }
                if (request.contact() != null) {
                    person.setContact(request.contact());
                }
                PersonRules.applyGrade(person, request.grade(), null, sheets.today(sheet));
                if (!store.save(person)) {
                    throw new StalePersonException();
                }
                audit.record("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                        + person.getName() + " grade=" + person.gradeOn(sheets.today(sheet))
                        + (person.isTermsChanged() ? " terms=" + (person.getTerms() == null ? "" : person.getTerms()) : "")
                        + (person.isContactChanged() ? " contact=" + (person.getContact() == null ? "" : person.getContact()) : ""));
                rollups.record(person.getSheetId(), request.user(), before);
                updated++;
                continue;
            }
            Person person = store.add(sheet, request.name(), request.grade(),
                    request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact());
            audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                    + person.getName() + " grade=" + request.grade()
                    + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                    + (person.getContact() == null ? "" : " contact=" + person.getContact()));
            rollups.record(person.getSheetId(), request.user(), before);
            added++;
        }
        result.set(new ImportResult(added, updated));
    }
}
