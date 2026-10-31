package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A generate needs two dates, from on or before to; a blank one is the sheet's today. */
public class ValidateGeneratePeriods {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GeneratePeriodsRequest request,
            Sheets sheets, Out<GeneratePeriodsRequest> body) throws InvalidSheetRequestException {
        if (request == null) {
            throw new InvalidSheetRequestException("A generate needs its dates");
        }
        LocalDate from = date(request.from(), sheetId, sheets);
        LocalDate to = date(request.to(), sheetId, sheets);
        if (from.isAfter(to)) {
            throw new InvalidSheetRequestException("The from-date must be on or before the to-date");
        }
        body.set(new GeneratePeriodsRequest(from.toString(), to.toString(), request.user()));
    }

    private static LocalDate date(String raw, String sheetId, Sheets sheets) throws InvalidSheetRequestException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return sheets.today(sheetId);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + value);
        }
    }
}
