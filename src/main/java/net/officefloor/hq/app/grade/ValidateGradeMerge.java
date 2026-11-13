package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade merge by anyone but a steward, of a grade the sheet does not have, or of a grade into itself. */
public class ValidateGradeMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeMergeRequest request,
            Out<GradeMergeInput> body, RoleStore roles) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can merge grades");
        }
        String from = request.from() == null ? "" : request.from().trim();
        String into = request.into() == null ? "" : request.into().trim();
        if (!GradeStore.GRADES.contains(from) || !GradeStore.GRADES.contains(into)) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
        if (from.equals(into)) {
            throw new InvalidSheetException("A grade cannot be merged into itself");
        }
        body.set(new GradeMergeInput(from, into, user));
    }
}
