package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A person's fields: a name (required), a grade, one of A, B or C, and the grade's from-date (blank, or a date as
 *  YYYY-MM-DD) and their own terms (blank for none, or a whole number of days). Publishes them trimmed, with the user making the change. */
public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern DAYS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> person,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = PersonName.of(request == null ? null : request.name());
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("Choose a grade: A, B or C");
        }
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Enter the grade's from-date as YYYY-MM-DD");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !DAYS.matcher(terms).matches()) {
            throw new BadRequestException("Terms are a whole number of days, or blank for none");
        }
        person.set(new PersonRequest(name, grade, by.name(), request.version(), gradeFrom,
                terms.isEmpty() ? "" : String.valueOf(Integer.parseInt(terms))));
        changedBy.set(by);
    }
}
