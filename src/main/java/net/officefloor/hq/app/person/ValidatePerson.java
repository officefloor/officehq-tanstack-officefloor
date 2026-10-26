package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    // The name is kept without surrounding spaces; a blank one is refused.
    public void validate(@RequestBody PersonRequest body, Out<PersonRequest> request) throws InvalidPersonException {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        String grade = body == null || body.grade() == null ? "" : body.grade().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("Type a name.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        // A blank from-date is kept null: the sheet's today, filled in when stored.
        String from = body.gradeFrom() == null ? "" : body.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date (yyyy-mm-dd): " + from);
            }
        }
        String terms = body.terms() == null ? "" : body.terms().trim();
        if (!terms.isEmpty() && !DAYS.matcher(terms).matches()) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more.");
        }
        request.set(new PersonRequest(name, grade, body.version(), from.isEmpty() ? null : from,
                terms.isEmpty() ? null : Integer.toString(Integer.parseInt(terms))));
    }
}
