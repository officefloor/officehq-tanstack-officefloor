package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Only a steward merges, two different known grades, and not into a retired grade. */
public class ValidateGradeMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeMergeRequest request,
            Out<ValidGradeMerge> body, Settings settings, RetiredGrades grades)
            throws InvalidGradeException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidGradeException("Only a steward may merge grades");
        }
        String from = request.from() == null ? "" : request.from().trim();
        String into = request.into() == null ? "" : request.into().trim();
        if (!RetiredGrades.GRADES.contains(from) || !RetiredGrades.GRADES.contains(into)) {
            throw new InvalidGradeException("Grade must be A, B or C");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself");
        }
        if (grades.isRetired(sheetId, into)) {
            throw new InvalidGradeException("Grade " + into + " is retired");
        }
        body.set(new ValidGradeMerge(from, into, request.user()));
    }
}
