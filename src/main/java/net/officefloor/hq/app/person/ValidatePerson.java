package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the person form: a name is required (trimmed; unique on the sheet, checked when saved), the grade
 *  must be one of the sheet's grades, and a from-date is yyyy-mm-dd (blank for the sheet's today), and the terms
 *  a whole number of days from 0 to 3650 (blank for none), and the contact is trimmed and
 *  lower-cased (blank for none; unique on the sheet, checked when saved). */
public class ValidatePerson {
    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> person) throws BadRequestException {
        person.set(NewPerson.check(request));
    }
}
