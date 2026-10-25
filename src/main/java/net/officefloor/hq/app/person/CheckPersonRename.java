package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses renaming a person to a name another person on the sheet already has. */
public class CheckPersonRename {
    public void check(@Val SheetResponse sheet, @Val PersonRequest request, @Val Person person,
            PersonRepository repository) throws BadRequestException {
        if (repository.nameTaken(sheet.id(), request.name(), person.getId())) {
            throw new BadRequestException(PersonRepository.NAME_TAKEN);
        }
    }
}
