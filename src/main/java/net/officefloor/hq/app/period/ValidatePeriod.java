package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A period needs a name and two dates, from on or before to. */
public class ValidatePeriod {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PeriodRequest request,
            Out<PeriodRequest> body, Out<Period> period) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetRequestException("A period needs a name");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from.isAfter(to)) {
            throw new InvalidSheetRequestException("A period's from-date must be on or before its to-date");
        }
        body.set(request);
        period.set(new Period(sheetId, name, from, to));
    }

    private static LocalDate date(String raw) throws InvalidSheetRequestException {
        String value = raw == null ? "" : raw.trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + value);
        }
    }
}
