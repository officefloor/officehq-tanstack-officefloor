package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.Grades;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateInlineGrade {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody InlineGradeRequest request, Grades grades,
            Out<InlineGradeRequest> body)
            throws InvalidPersonException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheet, grade)) {
            throw new InvalidPersonException("Grade must be one of the sheet's grades");
        }
        body.set(new InlineGradeRequest(grade));
    }
}
