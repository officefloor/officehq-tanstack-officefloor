package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the person form: a name is required (trimmed; unique on the sheet, checked when saved), the grade
 *  must be A, B or C, and a from-date is yyyy-mm-dd (blank for the sheet's today), and the terms
 *  a whole number of days from 0 to 3650 (blank for none). */
public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern TERMS = Pattern.compile("\\d{1,4}");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> person) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new BadRequestException("from-date must be yyyy-mm-dd");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && (!TERMS.matcher(terms).matches() || Integer.parseInt(terms) > 3650)) {
            throw new BadRequestException("terms must be a whole number of days from 0 to 3650");
        }
        person.set(new PersonRequest(name, grade, request.version(), from,
                terms.isEmpty() ? "" : String.valueOf(Integer.parseInt(terms))));
    }
}
