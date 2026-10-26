package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses renaming a person to a name another person on the sheet (or anyone on another sheet of its tree) already
 *  has, keeping a person whose alias is used on another sheet of the tree, or giving them a contact another person has. */
public class CheckPersonRename {
    public void check(@Val SheetResponse sheet, @Val PersonRequest request, @Val Person person,
            PersonRepository repository) throws BadRequestException {
        if (repository.nameTaken(sheet.id(), request.name(), person.getId())) {
            throw new BadRequestException(PersonRepository.NAME_TAKEN);
        }
        for (String alias : person.getAliases()) {
            if (repository.usedInTree(sheet.id(), alias, person.getId())) {
                throw new BadRequestException("The alias " + alias + " is already in use on another sheet of this tree");
            }
        }
        if (repository.contactTaken(sheet.id(), request.contactOrNull(), person.getId())) {
            throw new BadRequestException(PersonRepository.CONTACT_TAKEN);
        }
    }
}
