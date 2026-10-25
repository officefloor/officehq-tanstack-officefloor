package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the holiday and re-fixes the due-by of the claims it moves; a date already on the sheet's list is refused. */
public class BuildHoliday {
    public void build(@Val SheetResponse sheet, @Val HolidayInput input, HolidayRepository repository,
            ClaimRepository claims, Out<Holiday> holiday) throws BadRequestException {
        if (repository.taken(sheet.id(), input.date())) {
            throw new BadRequestException(HolidayRepository.TAKEN);
        }
        repository.insert(sheet.id(), input.date());
        claims.refix(sheet.id(), input.date());
        holiday.set(new Holiday(sheet.id(), input.date()));
    }
}
