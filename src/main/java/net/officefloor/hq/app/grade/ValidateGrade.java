package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade other than A, B or C before anything is read. */
public class ValidateGrade {
    public void validate(@RequestBody GradeRequest request, Out<GradeRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!Settings.isGradeCode(grade)) throw new InvalidGradeException("A grade is one to three capital letters");
        body.set(new GradeRequest(grade));
    }
}
