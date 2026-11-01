package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade that is not one of the sheet's, or one already retired. */
public class ValidateRetireGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest request, Grades grades,
            Out<GradeRetirement> retirement, Out<Actor> actor) throws InvalidSheetRequestException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade must be one of the sheet's");
        }
        if (grades.isRetired(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade " + grade + " is already retired");
        }
        retirement.set(new GradeRetirement(sheetId, grade, request.user()));
        actor.set(new Actor(request.user()));
    }
}
