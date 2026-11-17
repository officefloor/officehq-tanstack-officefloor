package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the name is required (kept without the spaces around it), the grade one of the sheet's grades. */
public class ValidatePerson {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest request, GradeRepository grades,
            Out<PersonValues> values) throws InvalidPersonException {
        values.set(PersonValues.from(request, grades.grades(sheetId.trim())));
    }
}
