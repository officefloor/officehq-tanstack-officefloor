package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body by the person form's rules: the grade one of A, B or C, the from-date a date or blank; the selected
 *  person ids each once (a missing list is none). */
public class ValidateBulkGrade {
    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGradeValues> values) throws InvalidPersonException {
        if (request == null) {
            throw new InvalidPersonException("A grade is required");
        }
        // validated as the person form would, with a placeholder name
        PersonValues checked = PersonValues.from(new PersonRequest("-", request.grade(), request.by(), null, request.from(), null));
        List<Long> ids = request.personIds() == null ? List.of()
                : request.personIds().stream().filter(Objects::nonNull).distinct().toList();
        values.set(new BulkGradeValues(ids, checked.grade(), checked.gradeFrom(), checked.by()));
    }
}
