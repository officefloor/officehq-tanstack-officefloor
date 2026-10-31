package net.officefloor.hq.app.person;

import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkGrade {
    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGrade> body) throws InvalidSheetRequestException {
        if (request == null || request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the people to grade");
        }
        // Checked by the person rules, so a grade or date the person form refuses is refused here too.
        PersonRequest checked = PersonRequest.checked(
                new PersonRequest("-", request.grade(), request.user(), null, request.from(), null));
        body.set(new BulkGrade(new LinkedHashSet<>(request.personIds()), checked.grade(), checked.gradeFrom(), request.user()));
    }
}
