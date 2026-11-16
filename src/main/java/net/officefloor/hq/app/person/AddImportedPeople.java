package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the checked lines in the order pasted. A line matching a person updates them as the person form edits one:
 *  the grade recorded from the sheet's today (see ApplyPerson), the terms and contact set when the line gives them,
 *  written as PERSON_EDITED (see RecordPersonEdited). Any other adds a person as the form adds one, written as
 *  PERSON_ADDED (see RecordPersonAdded). Each then writes the figures it moved (see Rollup). */
public class AddImportedPeople {
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, @Val ImportMatches matches,
            PersonRepository people, SheetRepository sheets, Rollup rollup, Audit audit, Out<ImportPeopleResult> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException {
        String sheet = sheetId.trim();
        int added = 0;
        int updated = 0;
        for (int i = 0; i < imported.people().size(); i++) {
            PersonValues values = imported.people().get(i);
            Person matched = matches.people().get(i);
            RollupBefore before = rollup.take(sheet);
            if (matched == null) {
                Person person = people.add(sheet, values.name(), values.grade(), values.terms(), values.contact());
                new RecordPersonAdded().record(person, values, before, rollup, audit);
                added++;
            } else {
                PersonValues edit = edit(matched, values);
                Person person = ApplyPerson.edited(matched, edit, sheets.today(sheet));
                people.update(person, matched.version());
                new RecordPersonEdited().record(person, edit, new TermsBefore(matched.terms()),
                        new ContactBefore(matched.contact()), before, rollup, audit);
                updated++;
            }
        }
        result.set(new ImportPeopleResult(added, updated));
    }

    /** The edit a line makes to the person it matches: their name kept, the line's grade from the sheet's today, and
     *  the line's terms and contact where it gives them (else the person's own). */
    static PersonValues edit(Person person, PersonValues line) {
        return new PersonValues(person.name(), line.grade(), line.by(), person.version(), null,
                line.terms() != null ? line.terms() : person.terms(),
                line.contact() != null ? line.contact() : person.contact(), person.tags());
    }
}
