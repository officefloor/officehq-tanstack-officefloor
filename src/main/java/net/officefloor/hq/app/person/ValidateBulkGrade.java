package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The selected people (at least one), a grade, one of A, B or C, its from-date (blank, or a date as YYYY-MM-DD) and
 *  the user giving it. */
public class ValidateBulkGrade {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGrade> bulk, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.ids() != null) {
            request.ids().stream().filter(Objects::nonNull).forEach(selected::add);
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("Select the people to grade");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("Choose a grade: A, B or C");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Enter the grade's from-date as YYYY-MM-DD");
            }
        }
        bulk.set(new BulkGrade(selected, grade, from));
        changedBy.set(by);
    }
}
