package net.officefloor.hq.app.holiday;

import java.util.HashSet;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the holiday, then works out again the due-by of every claim not SETTLED or VOID due on or after it,
 *  over the holidays now on the sheet (see {@link ClaimStore#refixDueBy}). */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val HolidayInput holiday, HolidayStore store,
            ClaimStore claims) {
        String sheet = sheetId.trim();
        store.add(sheet, holiday.date());
        claims.refixDueBy(sheet, holiday.date(), new HashSet<>(store.list(sheet)));
    }
}
