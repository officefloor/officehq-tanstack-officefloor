package net.officefloor.hq.app.holiday;

import java.util.HashSet;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the holiday on the sheet; a date already on its list is refused. Then every claim of the sheet that is neither
 *  SETTLED nor VOID and whose due-by is on or after the holiday has its due-by worked out again from its date, with the terms
 *  it was fixed with, over the holidays now on the sheet (see ClaimRepository#refixDueBys). */
public class AddHoliday {
    public void add(@PathVariable("sheetId") String sheetId, @Val HolidayValues values, HolidayRepository holidays,
            ClaimRepository claims) throws InvalidHolidayException {
        String sheet = sheetId.trim();
        if (!holidays.add(sheet, values.date())) {
            throw new InvalidHolidayException(values.date() + " is already a holiday");
        }
        claims.refixDueBys(sheet, values.date(), new HashSet<>(holidays.list(sheet)));
    }
}
