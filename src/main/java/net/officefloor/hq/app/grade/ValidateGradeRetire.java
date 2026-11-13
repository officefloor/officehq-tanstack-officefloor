package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses retiring a grade by a viewer, a grade the sheet does not have, or one already retired. */
public class ValidateGradeRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRetireRequest request,
            Out<GradeRetireInput> body, RoleStore roles, GradeStore grades) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeStore.GRADES.contains(grade)) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
        if (grades.isRetired(sheet, grade)) {
            throw new InvalidSheetException("The grade " + grade + " is already retired");
        }
        body.set(new GradeRetireInput(grade, user));
    }
}
