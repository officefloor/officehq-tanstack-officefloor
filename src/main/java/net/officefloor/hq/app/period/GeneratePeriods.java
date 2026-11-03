package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Make one period per calendar month from the from-date's month to the to-date's month, each from
 *  the first to the last day of its month and named yyyy-mm. A month whose name is already a period,
 *  or whose days overlap a period on the sheet, is left out. A blank from-date is the sheet's today;
 *  a to-date before the from-date is refused. A viewer's generation simply makes nothing (0, nothing written); an
 *  archived sheet refuses it. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings, Audit audit,
            Out<GenerateResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        if (SettingsRepository.VIEWER.equals(settings.role(id, by))) {
            result.set(new GenerateResult(0));
            return;
        }
        String today = repository.today(id);
        LocalDate from = date(request == null ? null : request.from(), "from-date",
                today == null ? LocalDate.now() : LocalDate.parse(today));
        LocalDate to = date(request == null ? null : request.to(), "to-date", null);
        if (to.isBefore(from)) {
            throw new BadRequestException("to-date must not be before from-date");
        }
        result.set(new GenerateResult(months(id, by, YearMonth.from(from), YearMonth.from(to), repository, audit)));
    }

    /** Make the monthly periods from one month to another, both inclusive, leaving out a month already named or
     *  overlapping a period; answers how many were made. */
    static int months(String id, String by, YearMonth start, YearMonth end, PeriodRepository repository, Audit audit) {
        int made = 0;
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
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
        return made;
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
