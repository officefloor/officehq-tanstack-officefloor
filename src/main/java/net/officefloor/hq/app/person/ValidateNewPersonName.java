package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse adding a person whose name another person on the sheet already has. */
public class ValidateNewPersonName {
    public void validate(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository) throws DuplicatePersonNameException {
        if (repository.nameInUse(sheetId.trim(), request.name(), null)) {
            throw new DuplicatePersonNameException(request.name());
        }
    }
}
