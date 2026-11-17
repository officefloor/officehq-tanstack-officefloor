package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: both grades are one of the sheet's grades, and not the same grade. */
public class ValidateMergeGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeGradeRequest request, GradeRepository grades,
            Out<MergeGradeValues> values) throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!grades.isGrade(sheetId.trim(), from) || !grades.isGrade(sheetId.trim(), into)) {
            throw new InvalidGradeException("Grade must be one of the sheet's grades");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself");
        }
        values.set(new MergeGradeValues(from, into, request.by() == null ? "" : request.by().trim()));
    }
}
