package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Works out again the due-by of every claim on the sheet that is not SETTLED or VOID and is due by on or after the new
 * holiday: from the claim's date, with the terms it was fixed with, over the holidays now on the sheet.
 */
public class RefixDueBys {
    public void refix(@Val Holiday holiday, ClaimRepository claims) {
        claims.refixDueBys(holiday.sheetId(), holiday.date());
    }
}
