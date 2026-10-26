package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGeneratePeriods {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A from and to date (yyyy-mm-dd), the to-date on or after the from-date. Each month from the from-date's to
    // the to-date's becomes a period named yyyy-mm, unless its name is taken or it shares a day with a period.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GeneratePeriodsRequest body,
            Periods periods, Out<GeneratedPeriods> generatedOut) throws InvalidPeriodException {
        LocalDate from = date(body == null ? null : body.from(), "from");
        LocalDate to = date(body.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date.");
        }
        String sheet = sheetId.trim();
        List<Period> existing = periods.list(sheet);
        List<Period> made = new ArrayList<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            Period month = new Period(sheet, m.toString(), m.atDay(1), m.atEndOfMonth());
            boolean clash = existing.stream().anyMatch(p -> p.name().equals(month.name()) || p.overlaps(month));
            if (!clash) {
                made.add(month);
            }
        }
        generatedOut.set(new GeneratedPeriods(made));
    }

    private static LocalDate date(String typed, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(typed == null ? "" : typed.trim(), ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (yyyy-mm-dd).");
        }
    }
}
