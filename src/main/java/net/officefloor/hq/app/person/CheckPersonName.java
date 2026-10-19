package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses adding a person with a name already used on the sheet (ignoring case). */
public class CheckPersonName {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository) throws InvalidPersonException {
        if (repository.nameInUse(sheetId, request.name(), null)) {
            throw new InvalidPersonException("That name is already in use on this sheet.");
        }
    }
}
