package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a to-date before the from-date (blank: the sheet's today). Otherwise adds a period per calendar month from
 * the from-date's month to the to-date's, first to last day, named yyyy-mm, each recorded as PERIOD_ADDED; a month
 * whose name is in use on the sheet or whose days overlap a period is left out.
 */
public class GeneratePeriods {
    public void generate(@Val SheetResponse sheet, @Val GenerateRange range, @Val ChangedBy by,
            PeriodRepository repository, SheetRepository sheets, Audit audit, Out<GeneratePeriodsResponse> result) throws BadRequestException {
        LocalDate from = range.from() != null ? range.from() : sheets.today(sheet.id());
        if (range.to().isBefore(from)) {
            throw new BadRequestException("The to-date is on or after the from-date");
        }
        int made = 0;
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(range.to())); month = month
                .plusMonths(1)) {
            String name = month.toString();
            LocalDate first = month.atDay(1);
            LocalDate last = month.atEndOfMonth();
            if (repository.nameTaken(sheet.id(), name) || repository.overlaps(sheet.id(), first, last)) {
                continue;
            }
            new RecordPeriodAdded().record(repository.insert(sheet.id(), name, first, last), by, audit);
            made++;
        }
        result.set(new GeneratePeriodsResponse(made));
    }
}
