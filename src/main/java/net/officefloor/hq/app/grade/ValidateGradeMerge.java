package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the grades to merge from the body, trimmed; refuses one not shaped as a grade, and the same grade twice. */
public class ValidateGradeMerge {
    public void validate(@RequestBody GradeMergeRequest request, Out<GradeMergeRequest> body) throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!RetiredGrades.isCode(from) || !RetiredGrades.isCode(into)) {
            throw new InvalidGradeException("The grades must be the sheet's grades.");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself.");
        }
        body.set(new GradeMergeRequest(from, into));
    }
}
