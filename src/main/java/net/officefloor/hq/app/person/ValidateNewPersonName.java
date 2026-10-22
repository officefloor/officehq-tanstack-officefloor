package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse adding a person whose name, or contact, another person on the sheet already has, or whose name is a name
 *  or alias on another sheet of the sheet's tree. */
public class ValidateNewPersonName {
    public void validate(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository) throws DuplicatePersonNameException, DuplicatePersonContactException {
        if (repository.nameInUse(sheetId.trim(), request.name(), null)) {
            throw new DuplicatePersonNameException(request.name());
        }
        DuplicatePersonNameException.refuseInTree(sheetId.trim(), request.name(), repository);
        if (request.contact() != null && !request.contact().isEmpty()
                && repository.contactInUse(sheetId.trim(), request.contact(), null)) {
            throw new DuplicatePersonContactException(request.contact());
        }
    }
}
