package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    // The name is kept without surrounding spaces; a blank one is refused, as is a grade that is not one of the sheet's.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest body,
            net.officefloor.hq.app.grade.Grades grades, Out<PersonRequest> request) throws InvalidPersonException {
        request.set(PersonRules.parse(body, grades.all(sheetId.trim())));
    }
}
