package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        String name = PersonRequest.checkName(request == null ? null : request.name());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) throw new InvalidPersonException("A grade is one of A, B or C");
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("A from-date is a date, yyyy-MM-dd");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidPersonException("Terms are a whole number of days, 0 or more, or blank");
        }
        if (!terms.isEmpty()) terms = String.valueOf(Integer.parseInt(terms));
        body.set(new PersonRequest(name, grade, request.version(), gradeFrom, terms));
    }
}
