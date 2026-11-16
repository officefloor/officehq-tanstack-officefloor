package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the from and to dates are yyyy-MM-dd (blank for the sheet's today), and the to-date is not before
 *  the from-date. */
public class ValidateGeneratePeriods {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GeneratePeriodsRequest request,
            SheetRepository sheets, Out<GeneratePeriodsValues> values) throws InvalidPeriodException {
        String sheet = sheetId.trim();
        LocalDate from = date(request == null ? null : request.from(), "from", sheets, sheet);
        LocalDate to = date(request == null ? null : request.to(), "to", sheets, sheet);
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date may not be before the from-date");
        }
        String by = request == null || request.by() == null ? "" : request.by().trim();
        values.set(new GeneratePeriodsValues(from, to, by));
    }

    private static LocalDate date(String text, String which, SheetRepository sheets, String sheet)
            throws InvalidPeriodException {
        if (text == null || text.isBlank()) {
            return sheets.today(sheet);
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date yyyy-MM-dd");
        }
    }
}
