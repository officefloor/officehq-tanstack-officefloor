package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateInlineGrade {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody InlineGradeRequest request, Out<InlineGradeRequest> body)
            throws InvalidPersonException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        body.set(new InlineGradeRequest(grade));
    }
}
