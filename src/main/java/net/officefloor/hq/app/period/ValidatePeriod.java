package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A period's fields: a name (required, at most 200 characters), a from-date (yyyy-mm-dd; blank for the
 * sheet's today) and a to-date (yyyy-mm-dd, required).
 */
public class ValidatePeriod {

    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> period, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = request.name() == null ? "" : request.name().trim();
        String from = request.from() == null ? "" : request.from().trim();
        String to = request.to() == null ? "" : request.to().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Enter a period name");
        }
        if (name.length() > 200) {
            throw new BadRequestException("A period name is at most 200 characters");
        }
        if (to.isEmpty()) {
            throw new BadRequestException("Enter a to-date");
        }
        period.set(new PeriodInput(name, from.isEmpty() ? null : date(from), date(to)));
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
