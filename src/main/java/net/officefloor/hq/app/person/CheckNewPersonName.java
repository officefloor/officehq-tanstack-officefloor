package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A person being added may not take a name already on the sheet, or used as a name or alias on another sheet of its tree. */
public class CheckNewPersonName {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonNameException {
        if (people.nameTaken(sheetId.trim(), values.name(), 0) || people.treeTaken(sheetId.trim(), values.name())) {
            throw new DuplicatePersonNameException(values.name());
        }
    }
}
