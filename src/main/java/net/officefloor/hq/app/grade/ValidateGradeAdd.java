package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses adding a grade by a viewer, a code that is not one to three capital letters (once trimmed), or a
 *  grade the sheet already has. */
public class ValidateGradeAdd {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeAddRequest request,
            Out<GradeAddInput> body, RoleStore roles, GradeStore grades) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeStore.CODE.matcher(grade).matches()) {
            throw new InvalidSheetException("A grade must be one to three capital letters");
        }
        if (grades.has(sheet, grade)) {
            throw new InvalidSheetException("The grade " + grade + " is already a grade");
        }
        body.set(new GradeAddInput(grade, user));
    }
}
