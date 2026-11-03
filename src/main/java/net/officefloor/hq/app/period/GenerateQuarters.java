package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Make the four calendar quarters of a year (yyyy) as periods named <yyyy>-Q1 to <yyyy>-Q4, each as the period
 *  form adds it; a quarter whose name is already a period, or whose days overlap a period on the sheet, is left
 *  out. A year that is not yyyy is refused. */
public class GenerateQuarters {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFyRequest request,
            ServerHttpConnection connection, PeriodRepository repository, Audit audit, Out<GenerateResult> result)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new BadRequestException("year must be a year (yyyy)");
        }
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        int y = Integer.parseInt(year);
        int made = 0;
        for (int q = 1; q <= 4; q++) {
            String name = year + "-Q" + q;
            LocalDate first = YearMonth.of(y, q * 3 - 2).atDay(1);
            LocalDate last = YearMonth.of(y, q * 3).atEndOfMonth();
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
}
