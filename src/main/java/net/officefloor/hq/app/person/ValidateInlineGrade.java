package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade not shaped as one (one to three capital letters) before anything is read. */
public class ValidateInlineGrade {

    public void validate(@RequestBody InlineGradeRequest request, Out<InlineGradeRequest> body)
            throws InvalidPersonException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!net.officefloor.hq.app.setting.Settings.isGradeCode(grade)) throw new InvalidPersonException("A grade is one to three capital letters");
        body.set(new InlineGradeRequest(grade));
    }
}
