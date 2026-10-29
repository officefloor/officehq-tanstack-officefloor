package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRollover {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    // A year (yyyy), rolled over only by a steward, and only when the fiscal year FY<yyyy> has a period (one starting
    // within it). The next fiscal year is generated as the fiscal year generator does; each person's open figure at the
    // end of the old year's last period goes to the new year's first period (the one holding its first day).
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest body,
            Periods periods, Settings settings, Rollup rollup, ServerHttpConnection connection,
            Out<GeneratedPeriods> generatedOut, Out<Rollover> rolloverOut) throws InvalidPeriodException, PeriodRefusedException {
        String year = body == null || body.year() == null ? "" : body.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("The fiscal year must be a year (yyyy).");
        }
        String sheet = sheetId.trim();
        if (!Settings.STEWARD.equals(settings.role(sheet, Audit.user(connection).trim()))) {
            throw new PeriodRefusedException("Only a steward rolls the year over.");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        LocalDate from = start.atDay(1);
        LocalDate next = start.plusMonths(12).atDay(1);
        List<Period> existing = periods.list(sheet);
        List<Period> old = existing.stream().filter(p -> !p.from().isBefore(from) && p.from().isBefore(next)).toList();
        if (old.isEmpty()) {
            throw new PeriodRefusedException("The fiscal year FY" + year + " has no periods.");
        }
        Period last = old.stream().max(Comparator.comparing(Period::to)).orElseThrow();
        YearMonth newStart = start.plusMonths(12);
        GeneratedPeriods generated = GeneratedPeriods.months(sheet, existing, newStart, newStart.plusMonths(11));
        Period first = generated.periods().stream().filter(p -> p.contains(next)).findFirst()
                .orElseGet(() -> periods.on(sheet, next));
        generatedOut.set(generated);
        rolloverOut.set(new Rollover("FY" + year, first, rollup.personOpenAt(sheet, old, last.to())));
    }
}
