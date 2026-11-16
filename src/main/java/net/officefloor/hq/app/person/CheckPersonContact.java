package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A person being added may not take a contact already on the sheet. */
public class CheckPersonContact {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonContactException {
        if (values.contact() != null && people.contactTaken(sheetId.trim(), values.contact(), 0)) {
            throw new DuplicatePersonContactException(values.contact());
        }
    }
}
