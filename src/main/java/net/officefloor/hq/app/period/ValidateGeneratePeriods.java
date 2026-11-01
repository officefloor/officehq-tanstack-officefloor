package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGeneratePeriods {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody GeneratePeriodsRequest request,
            SheetClock clock, Out<GeneratePeriodsInput> body) throws InvalidPeriodException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        LocalDate start;
        LocalDate end;
        try {
            start = from.isEmpty() ? clock.today(sheet) : LocalDate.parse(from);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Not a valid from-date: " + from);
        }
        try {
            end = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Not a valid to-date: " + to);
        }
        if (end.isBefore(start)) {
            throw new InvalidPeriodException("The to-date may not be before the from-date");
        }
        body.set(new GeneratePeriodsInput(start, end));
    }
}
