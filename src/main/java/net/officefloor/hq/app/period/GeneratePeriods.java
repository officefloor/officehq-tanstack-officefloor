package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Make one period per calendar month from the from-date's month to the to-date's month, each from
 *  the first to the last day of its month and named yyyy-mm. A month whose name is already a period,
 *  or whose days overlap a period on the sheet, is left out. A blank from-date is the sheet's today;
 *  a to-date before the from-date is refused. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request,
            ServerHttpConnection connection, PeriodRepository repository, Audit audit, Out<GenerateResult> result)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        String today = repository.today(id);
        LocalDate from = date(request == null ? null : request.from(), "from-date",
                today == null ? LocalDate.now() : LocalDate.parse(today));
        LocalDate to = date(request == null ? null : request.to(), "to-date", null);
        if (to.isBefore(from)) {
            throw new BadRequestException("to-date must not be before from-date");
        }
        int made = 0;
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            String name = m.toString();
            LocalDate first = m.atDay(1);
            LocalDate last = m.atEndOfMonth();
            if (repository.named(id, name) || repository.overlaps(id, first, last)) {
                continue;
            }
            Period saved = repository.add(id, name, first, last);
            audit.record("PERIOD_ADDED sheet=" + id + " by=" + by + " period=" + saved.name() + " from="
                    + saved.from() + " to=" + saved.to());
            made++;
        }
        result.set(new GenerateResult(made));
    }

    private static LocalDate date(String value, String what, LocalDate blank) throws BadRequestException {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            if (blank != null) {
                return blank;
            }
            throw new BadRequestException(what + " is required");
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(what + " must be yyyy-mm-dd");
        }
    }
}
