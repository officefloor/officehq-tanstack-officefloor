package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a period whose to-date is before its from-date (blank: the sheet's today), whose name is in use on the
 * sheet, or whose dates overlap another period's. Publishes the period to add, its dates resolved.
 */
public class CheckPeriod {
    public void check(@Val SheetResponse sheet, @Val PeriodInput input, PeriodRepository repository,
            SheetRepository sheets, Out<PeriodDates> dates) throws BadRequestException {
        LocalDate from = input.from() != null ? input.from() : sheets.today(sheet.id());
        if (input.to().isBefore(from)) {
            throw new BadRequestException("A period's to-date is on or after its from-date");
        }
        if (repository.nameTaken(sheet.id(), input.name())) {
            throw new BadRequestException(PeriodRepository.NAME_TAKEN);
        }
        if (repository.overlaps(sheet.id(), from, input.to())) {
            throw new BadRequestException("A period may not overlap another period");
        }
        dates.set(new PeriodDates(from, input.to()));
    }
}
