package net.officefloor.hq.app.holiday;

import java.util.HashSet;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.plugin.variable.Val;

public class RefixDueBys {
    // Every claim not SETTLED or VOID due on or after the new holiday is due again by the terms-th business day after
    // its date, with the terms it was fixed with, over the holidays now on the sheet.
    public void refix(@Val Holiday holiday, Holidays holidays, Claims claims) {
        claims.refixDueBys(holiday.sheetId(), holiday.date(), new HashSet<>(holidays.list(holiday.sheetId())));
    }
}
