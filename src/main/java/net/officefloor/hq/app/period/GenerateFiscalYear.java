package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Make the twelve monthly periods of a fiscal year (FY<yyyy>: from the first day of the sheet's fiscal year start
 *  month in that calendar year) exactly as the period generator does. A year that is not yyyy is refused. */
public class GenerateFiscalYear {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFyRequest request,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings, Audit audit,
            Out<GenerateResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new BadRequestException("fiscal year must be a year (yyyy)");
        }
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(id));
        result.set(new GenerateResult(GeneratePeriods.months(id, by, start, start.plusMonths(11), repository, audit)));
    }
}
