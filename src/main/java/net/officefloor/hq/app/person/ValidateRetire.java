package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The date to retire from: the one given, or the sheet's today when blank. */
public class ValidateRetire {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody RetireRequest request, SheetClock clock,
            Out<LocalDate> from) throws InvalidPersonException {
        String date = request == null || request.from() == null ? "" : request.from().trim();
        try {
            from.set(date.isEmpty() ? clock.today(sheet) : LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date: " + date);
        }
    }
}
