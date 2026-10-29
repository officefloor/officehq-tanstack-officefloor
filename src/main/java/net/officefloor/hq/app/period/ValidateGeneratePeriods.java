package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGeneratePeriods {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // A from and to date (yyyy-mm-dd), the to-date on or after the from-date. Each month from the from-date's to
    // the to-date's becomes a period named yyyy-mm, unless its name is taken or it shares a day with a period. A viewer
    // changes nothing: they are told 0, and nothing is stored or recorded.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GeneratePeriodsRequest body,
            Periods periods, Settings settings, ServerHttpConnection connection, Out<GeneratedPeriods> generatedOut)
            throws InvalidPeriodException {
        LocalDate from = date(body == null ? null : body.from(), "from");
        LocalDate to = date(body.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date.");
        }
        String sheet = sheetId.trim();
        if (settings.isViewer(sheet, Audit.user(connection))) {
            generatedOut.set(new GeneratedPeriods(List.of()));
            return;
        }
        generatedOut.set(GeneratedPeriods.months(sheet, periods.list(sheet), YearMonth.from(from), YearMonth.from(to)));
    }

    private static LocalDate date(String typed, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(typed == null ? "" : typed.trim(), ISO);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (yyyy-mm-dd).");
        }
    }
}
