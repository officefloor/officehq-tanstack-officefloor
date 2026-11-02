package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Collection;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody PersonRequest request, Grades grades,
            Out<PersonRequest> body) throws InvalidPersonException {
        body.set(check(request, grades.all(sheet)));
    }

    /** The form's rules for the fields alone: the request trimmed as the form would save it; shared with the import. */
    static PersonRequest check(PersonRequest request, Collection<String> grades) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required");
        }
        if (!grades.contains(grade)) {
            throw new InvalidPersonException("Grade must be one of the sheet's grades");
        }
        String from = date(request.gradeFrom());
        String termsFrom = date(request.termsFrom());
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty()) {
            if (!terms.matches("[0-9]{1,6}")) {
                throw new InvalidPersonException("The terms must be a whole number of days, 0 or more, or blank");
            }
            terms = Integer.toString(Integer.parseInt(terms));
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(Locale.ROOT);
        return new PersonRequest(name, grade, request.version(), from, terms, contact, request.tags(), termsFrom);
    }

    /** The from-date the form gave (yyyy-mm-dd); null when blank. */
    private static String date(String given) throws InvalidPersonException {
        String from = given == null ? "" : given.trim();
        if (from.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(from).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid from-date: " + from);
        }
    }
}
