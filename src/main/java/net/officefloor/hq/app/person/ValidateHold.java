package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The hold to add: from the date given (the sheet's today when blank) to the to-date given (none when blank), which may not be before it. */
public class ValidateHold {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody HoldRequest request, SheetClock clock,
            Out<Hold> hold) throws InvalidPersonException {
        hold.set(of(sheet, request, clock));
    }

    /** The hold the request asks for, refused on a bad date or a to-date before the from-date; the bulk hold uses it too. */
    static Hold of(String sheet, HoldRequest request, SheetClock clock) throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        LocalDate start = from.isEmpty() ? clock.today(sheet) : parse(from);
        LocalDate end = to.isEmpty() ? null : parse(to);
        if (end != null && end.isBefore(start)) {
            throw new InvalidPersonException("A hold may not end before it starts");
        }
        return new Hold(start.toString(), end == null ? null : end.toString());
    }

    private static LocalDate parse(String date) throws InvalidPersonException {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date: " + date);
        }
    }
}
