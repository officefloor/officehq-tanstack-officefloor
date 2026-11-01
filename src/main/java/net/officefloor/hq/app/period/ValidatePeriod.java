package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody PeriodRequest request, SheetClock clock,
            Out<PeriodInput> body) throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A period needs a name");
        }
        if (name.length() > 200) {
            throw new InvalidPeriodException("A period's name is at most 200 characters");
        }
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
            throw new InvalidPeriodException("A period's to-date may not be before its from-date");
        }
        body.set(new PeriodInput(name, start, end));
    }
}
