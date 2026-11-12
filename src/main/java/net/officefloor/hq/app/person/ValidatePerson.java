package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest request, Out<PersonRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        if (request == null || request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidSheetException("A name is required");
        }
        if (request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        try {
            if (!from.isEmpty()) {
                LocalDate.parse(from);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + from);
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidSheetException("The terms must be a whole number of days, 0 or more, or blank");
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        // Stored and compared without the spaces around the name; a blank from-date as none.
        body.set(new PersonRequest(request.name().trim(), request.grade(), from.isEmpty() ? null : from,
                request.user(), request.version(),
                terms.isEmpty() ? null : String.valueOf(Integer.parseInt(terms)), contact.isEmpty() ? null : contact));
    }
}
