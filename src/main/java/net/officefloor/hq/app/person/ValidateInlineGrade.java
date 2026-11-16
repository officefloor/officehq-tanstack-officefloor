package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body by the person form's rules: the grade one of A, B or C. */
public class ValidateInlineGrade {
    public void validate(@RequestBody InlineGradeRequest request, Out<InlineGradeValues> values) throws InvalidPersonException {
        if (request == null) {
            throw new InvalidPersonException("A grade is required");
        }
        // validated as the person form would, with a placeholder name
        PersonValues checked = PersonValues.from(new PersonRequest("-", request.grade(), request.by(), null, null, null, null, null));
        values.set(new InlineGradeValues(checked.grade(), checked.by()));
    }
}
