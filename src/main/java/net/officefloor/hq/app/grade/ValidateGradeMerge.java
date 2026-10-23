package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and two different grades of
 *  the sheet. */
public class ValidateGradeMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeMergeRequest request,
            GradeRepository grades, Out<GradeMergeRequest> body)
            throws InvalidGradeException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidGradeException("Sign in before merging a grade.");
        }
        String from = request.from() == null ? "" : request.from().trim();
        String into = request.into() == null ? "" : request.into().trim();
        if (!grades.isGrade(sheetId.trim(), from) || !grades.isGrade(sheetId.trim(), into)) {
            throw new InvalidGradeException("The grades must be grades of the sheet.");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("Choose two different grades to merge.");
        }
        body.set(new GradeMergeRequest(request.user().trim(), from, into));
    }
}
