package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a person per imported line, in order. Every line is first checked as the person form checks a new person
 * (name or alias in use on the sheet or its tree, contact another person's); one refusal refuses them all before
 * anything is written. Each is then added as the form adds them: PERSON_ADDED, then the rollup lines it moved.
 */
public class AddImportedPeople {

    public void add(@Val SheetResponse sheet, @Val ImportedPeople imported, @Val ChangedBy by,
            PersonRepository repository, Rollup rollup, Audit audit, Out<ImportPeopleResponse> response)
            throws BadRequestException {
        int n = 0;
        for (PersonRequest request : imported.people()) {
            n++;
            if (repository.nameTaken(sheet.id(), request.name(), null)) {
                throw new BadRequestException("Line " + n + ": " + PersonRepository.NAME_TAKEN);
            }
            if (repository.contactTaken(sheet.id(), request.contactOrNull(), null)) {
                throw new BadRequestException("Line " + n + ": " + PersonRepository.CONTACT_TAKEN);
            }
        }
        for (PersonRequest request : imported.people()) {
            RollupSnapshot before = rollup.snapshot(sheet.id());
            Person person = repository.insert(sheet.id(), request.name(), request.grade(), request.termsDays(),
                    request.contactOrNull());
            new RecordPersonAdded().record(person, by, audit);
            rollup.record(sheet.id(), before, by);
        }
        response.set(new ImportPeopleResponse(imported.people().size()));
    }
}
