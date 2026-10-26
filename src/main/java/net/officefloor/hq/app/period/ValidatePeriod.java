package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A period's fields: a name (required, at most 200 characters), a from-date (yyyy-mm-dd; blank for the
 * sheet's today) a to-date (yyyy-mm-dd, required) and a cap on each person's OPEN or PENDING claims in it (a whole number of
 * at least 1; blank for none).
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
        String cap = request.cap() == null ? "" : request.cap().trim();
        period.set(new PeriodInput(name, from.isEmpty() ? null : date(from), date(to), cap.isEmpty() ? null : cap(cap)));
        changedBy.set(by);
    }

    private static Integer cap(String text) throws BadRequestException {
        if (!text.matches("[0-9]{1,9}") || Integer.parseInt(text) < 1) {
            throw new BadRequestException("A cap is a whole number of at least 1, or blank for none");
        }
        return Integer.valueOf(text);
    }

    private static LocalDate date(String text) throws BadRequestException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}
