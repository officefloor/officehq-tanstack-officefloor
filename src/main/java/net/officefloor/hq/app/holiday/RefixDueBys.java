package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;

/** A holiday added moves the due-by of every claim not SETTLED or VOID due by it or later: see
 *  {@link ClaimRepository#refixDueBys}. */
public class RefixDueBys {
    public void refix(@Val Holiday holiday, ClaimRepository claims) {
        claims.refixDueBys(holiday.sheetId(), LocalDate.parse(holiday.date()));
    }
}
