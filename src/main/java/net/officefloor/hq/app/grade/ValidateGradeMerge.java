package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a merge by anyone but a steward, on an archived sheet, of a grade into itself, of a grade
 *  not one of the sheet's, or into a retired grade. */
public class ValidateGradeMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeMergeRequest request,
            Settings settings, Sheets sheets, Grades grades, Out<GradeMerge> merge)
            throws InvalidSheetRequestException {
        if (request == null || request.user() == null || settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can merge grades");
        }
        if (sheets.archived(sheetId)) {
            throw new InvalidSheetRequestException("The sheet is archived");
        }
        String from = request.from() == null ? "" : request.from().trim();
        String into = request.into() == null ? "" : request.into().trim();
        if (!grades.isGrade(sheetId, from) || !grades.isGrade(sheetId, into)) {
            throw new InvalidSheetRequestException("The grades must be grades of the sheet");
        }
        if (from.equals(into)) {
            throw new InvalidSheetRequestException("A grade cannot be merged into itself");
        }
        grades.refuseRetired(sheetId, into);
        merge.set(new GradeMerge(sheetId, from, into, request.user()));
    }
}
