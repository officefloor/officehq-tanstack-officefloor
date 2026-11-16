package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A person being added may not take a name already on the sheet. */
public class CheckNewPersonName {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonNameException {
        if (people.nameTaken(sheetId.trim(), values.name(), 0)) {
            throw new DuplicatePersonNameException(values.name());
        }
    }
}
