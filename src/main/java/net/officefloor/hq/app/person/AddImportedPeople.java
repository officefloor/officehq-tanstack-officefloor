package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A line whose name matches a person on the sheet (by name or alias, as names are compared) edits
 *  them as the person form would: their name kept, the line's grade from the sheet's today, and the
 *  line's terms and contact when it gives them (blank: kept). Any other line adds a person as the
 *  form would. Every line is checked first, so one refused line refuses the whole import before
 *  anything is saved; then each is saved in order, audited PERSON_EDITED or PERSON_ADDED and then
 *  the figures it moved, as the form does. */
public class AddImportedPeople {
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, People people,
            Sheets sheets, Rollup rollup, Audit audit, Out<ImportPeopleResult> result)
            throws InvalidSheetRequestException, PersonConflictException {
        List<PersonRequest> requests = new ArrayList<>();
        List<Person> targets = new ArrayList<>();
        Set<Long> matched = new HashSet<>();
        int n = 0;
        for (PersonRequest line : imported.lines()) {
            n++;
            Person existing = people.named(sheetId, line.name());
            try {
                if (existing == null) {
                    requests.add(line);
                    targets.add(BuildPerson.built(sheetId, line, people));
                    continue;
                }
                if (!matched.add(existing.getId())) {
                    throw new InvalidSheetRequestException("the person is repeated in the import: " + existing.getName());
                }
                PersonRequest edit = new PersonRequest(existing.getName(), line.grade(), line.user(), null, null,
                        line.termsDays() != null ? line.terms()
                                : existing.getTerms() != null ? String.valueOf(existing.getTerms()) : "",
                        line.contactKey() != null ? line.contactKey() : existing.getContact());
                ApplyPerson.check(edit, existing, people);
                requests.add(edit);
                targets.add(existing);
            } catch (InvalidSheetRequestException e) {
                throw new InvalidSheetRequestException("Line " + n + ": " + e.getMessage());
            }
        }
        SavePerson save = new SavePerson();
        RecordPersonAdded added = new RecordPersonAdded();
        RecordPersonEdited edited = new RecordPersonEdited();
        int addedCount = 0;
        for (int i = 0; i < targets.size(); i++) {
            Person person = targets.get(i);
            PersonRequest request = requests.get(i);
            Snapshot before = rollup.snapshot(sheetId);
            if (person.getId() == null) {
                save.save(person, people);
                added.record(request, person, audit);
                addedCount++;
            } else {
                ApplyPerson.applied(request, person, sheets);
                save.save(person, people);
                edited.record(request, person, audit);
            }
            rollup.record(sheetId, before, imported.user());
        }
        result.set(new ImportPeopleResult(addedCount, targets.size() - addedCount));
    }
}
