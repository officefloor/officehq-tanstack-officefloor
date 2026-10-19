package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PersonRequest request,
            SheetClock clock, Out<PersonRequest> body) throws InvalidPersonException {
        String name = request == null ? null : request.name();
        String grade = request == null ? null : request.grade();
        if (name == null || name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (grade == null || !GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().strip();
        LocalDate gradeFrom;
        if (from.isEmpty()) {
            gradeFrom = clock.today(sheetId);
        } else {
            try {
                gradeFrom = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date (yyyy-MM-dd).");
            }
        }
        // Stored without surrounding spaces.
        body.set(new PersonRequest(name.strip(), grade, request.version(), gradeFrom.toString()));
    }
}
