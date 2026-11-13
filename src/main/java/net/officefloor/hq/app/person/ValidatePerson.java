package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest request, Out<PersonRequest> body, RoleStore roles, GradeStore grades) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        body.set(PersonRules.check(request, grades.grades(sheetId.trim())));
    }
}
