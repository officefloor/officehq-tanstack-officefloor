package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses adding a person whose name is already in use on the sheet, or whose contact another person has. */
public class CheckPersonName {
    public void check(@Val SheetResponse sheet, @Val PersonRequest request, PersonRepository repository)
            throws BadRequestException {
        if (repository.nameTaken(sheet.id(), request.name(), null)) {
            throw new BadRequestException(PersonRepository.NAME_TAKEN);
        }
        if (repository.contactTaken(sheet.id(), request.contactOrNull(), null)) {
            throw new BadRequestException(PersonRepository.CONTACT_TAKEN);
        }
    }
}
