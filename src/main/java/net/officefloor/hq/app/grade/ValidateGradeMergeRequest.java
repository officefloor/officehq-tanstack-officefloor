package net.officefloor.hq.app.grade;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a grade that is not one of the sheet's, the same grade twice, or a merge into a retired grade. */
public class ValidateGradeMergeRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid GradeMergeRequest request, GradeRepository grades,
            Out<GradeMerge> merge) throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim().toUpperCase();
        String into = request == null || request.into() == null ? "" : request.into().trim().toUpperCase();
        if (!grades.exists(sheet.id(), from) || !grades.exists(sheet.id(), into)) {
            throw new InvalidGradeException("The grades must be the sheet's grades.");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself.");
        }
        if (grades.isRetired(sheet.id(), into)) {
            throw new InvalidGradeException("The grade " + into + " is retired.");
        }
        merge.set(new GradeMerge(sheet.id(), from, into));
    }
}
