package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade other than A, B or C, or the same grade twice, before anything is read. */
public class ValidateGradeMerge {
    public void validate(@RequestBody GradeMergeRequest request, Out<GradeMergeRequest> body)
            throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!Settings.GRADES.contains(from) || !Settings.GRADES.contains(into)) {
            throw new InvalidGradeException("A grade is one of A, B or C");
        }
        if (from.equals(into)) throw new InvalidGradeException("A grade cannot be merged into itself");
        body.set(new GradeMergeRequest(from, into));
    }
}
