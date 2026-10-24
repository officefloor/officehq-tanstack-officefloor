package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the holiday to the sheet; refuses a date already on its list. Every claim on the sheet has its
 * due-by worked out again, with the terms it was fixed with and the holidays now. */
public class ApplyHoliday {
    public void apply(@Val LocalDate date, @Val Sheet sheet, HolidayRepository repository,
            ClaimRepository claims, Out<Holiday> holidayOut)
            throws InvalidHolidayException {
        if (repository.existsBySheetIdAndDate(sheet.getId(), date)) {
            throw new InvalidHolidayException("That date is already a holiday.");
        }
        holidayOut.set(repository.saveAndFlush(new Holiday(sheet.getId(), date)));
        Set<LocalDate> holidays = repository.datesOf(sheet.getId());
        List<Claim> all = claims.findBySheetIdOrderByIdDesc(sheet.getId());
        all.forEach(c -> c.reworkDueBy(holidays));
        claims.saveAllAndFlush(all);
    }
}
