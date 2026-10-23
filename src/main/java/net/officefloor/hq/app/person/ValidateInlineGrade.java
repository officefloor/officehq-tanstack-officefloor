package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a grade of A, B or C. */
public class ValidateInlineGrade {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody InlineGradeRequest request, Out<InlineGradeRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        body.set(new InlineGradeRequest(request.user().trim(), grade, request.version()));
    }
}
