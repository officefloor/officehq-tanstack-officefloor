package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest request,
            GradeRepository grades, Out<PersonRequest> body)
            throws InvalidPersonException {
        body.set(PersonForm.check(request, grades.codes(sheetId.trim())));
    }
}
