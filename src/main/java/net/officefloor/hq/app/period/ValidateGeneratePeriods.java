package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The dates to generate between: a from-date (yyyy-mm-dd; blank for the sheet's today) and a to-date (required). */
public class ValidateGeneratePeriods {

    public void validate(@RequestBody GeneratePeriodsRequest request, Out<GenerateRange> range,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new BadRequestException("Enter a to-date");
        }
        range.set(new GenerateRange(from.isEmpty() ? null : date(from), date(to)));
        changedBy.set(by);
    }

    private static LocalDate date(String text) throws BadRequestException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}
