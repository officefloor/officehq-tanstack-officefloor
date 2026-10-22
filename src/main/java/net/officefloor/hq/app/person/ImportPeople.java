package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Check every imported person against the sheet first, as the person form would: their name not a name or alias
 *  on the sheet or another sheet of its tree, their contact not another person's. Any failure refuses the whole
 *  import, naming the line, and nothing is written. Otherwise each is added in order just as the form adds them:
 *  their PERSON_ADDED line and then how the sheet's figures moved over it. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId, @Val ImportInput input,
            PersonRepository repository, Rollups rollups, Audit audit, Out<ImportResponse> result)
            throws InvalidPersonException {
        String sheet = sheetId.trim();
        for (ImportLine line : input.lines()) {
            PersonRequest request = line.person();
            try {
                if (repository.nameInUse(sheet, request.name(), null)) {
                    throw new DuplicatePersonNameException(request.name());
                }
                DuplicatePersonNameException.refuseInTree(sheet, request.name(), repository);
                if (!request.contact().isEmpty() && repository.contactInUse(sheet, request.contact(), null)) {
                    throw new DuplicatePersonContactException(request.contact());
                }
            } catch (DuplicatePersonNameException | DuplicatePersonContactException e) {
                throw new InvalidPersonException("Line " + line.number() + ": " + e.getMessage());
            }
        }
        for (ImportLine line : input.lines()) {
            PersonRequest request = line.person();
            Rollup before = rollups.take(sheet);
            Person person = repository.insert(sheet, request.name(), request.grade(),
                    request.terms().isEmpty() ? null : Integer.valueOf(request.terms()),
                    request.contact().isEmpty() ? null : request.contact());
            AuditPersonAdded.record(request, person, audit);
            rollups.record(sheet, input.user(), before);
        }
        result.set(new ImportResponse(input.lines().size()));
    }
}
