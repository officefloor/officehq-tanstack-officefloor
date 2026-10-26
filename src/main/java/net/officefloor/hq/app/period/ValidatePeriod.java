package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A period needs a name not already used on the sheet, and a from and to date (yyyy-mm-dd), the
    // to-date on or after the from-date, sharing no day with another of the sheet's periods.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PeriodRequest body, Periods periods,
            Out<Period> periodOut) throws InvalidPeriodException {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A period needs a name.");
        }
        LocalDate from = date(body.from(), "from");
        LocalDate to = date(body.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date.");
        }
        Period period = new Period(sheetId.trim(), name, from, to);
        for (Period other : periods.list(period.sheetId())) {
            if (other.name().equals(name)) {
                throw new InvalidPeriodException("There is already a period named " + name + ".");
            }
            if (other.overlaps(period)) {
                throw new InvalidPeriodException("The period overlaps " + other.name() + ".");
            }
        }
        periodOut.set(period);
    }

    private static LocalDate date(String typed, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(typed == null ? "" : typed.trim(), ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (yyyy-mm-dd).");
        }
    }
}
