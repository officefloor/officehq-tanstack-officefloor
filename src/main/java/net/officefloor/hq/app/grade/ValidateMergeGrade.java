package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a grade not a grade of the sheet, and for merging a grade into itself. */
public class ValidateMergeGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeGradeRequest request,
            GradeRepository grades, Out<MergeGradeRequest> body)
            throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!grades.isGrade(sheetId.trim(), from) || !grades.isGrade(sheetId.trim(), into)) {
            throw new InvalidGradeException("Not a grade of the sheet");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself");
        }
        body.set(new MergeGradeRequest(from, into));
    }
}
