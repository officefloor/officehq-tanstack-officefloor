package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePersonRequest {

    public void validate(@RequestBody @Valid PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        // The name is kept without surrounding spaces; a blank one is refused.
        String name = request.name() == null ? "" : request.name().strip();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim().toUpperCase();
        // the form of a grade here; whether the sheet has it is checked against the sheet
        if (!GradeRepository.CODE.matcher(grade).matches()) {
            throw new InvalidPersonException("The grade must be one to three letters.");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be yyyy-mm-dd.");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,5}")) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        String termsFrom = request.termsFrom() == null ? "" : request.termsFrom().trim();
        if (!termsFrom.isEmpty()) {
            try {
                LocalDate.parse(termsFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The terms' from-date must be yyyy-mm-dd.");
            }
        }
        String contact = request.contact() == null ? "" : request.contact().strip().toLowerCase(Locale.ROOT);
        String tags = request.tags() == null ? null : Claim.tags(request.tags());
        body.set(new PersonRequest(name, grade, from, request.version(),
                terms.isEmpty() ? "" : String.valueOf(Integer.parseInt(terms)), contact,
                request.tags() == null ? null : tags == null ? "" : tags, termsFrom));
    }
}
