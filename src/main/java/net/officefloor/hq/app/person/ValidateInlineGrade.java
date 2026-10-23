package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and one of the sheet's grades. */
public class ValidateInlineGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody InlineGradeRequest request,
            GradeRepository grades, Out<InlineGradeRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidPersonException("The grade must be one of the sheet's grades.");
        }
        body.set(new InlineGradeRequest(request.user().trim(), grade, request.version()));
    }
}
