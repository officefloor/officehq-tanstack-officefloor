package net.officefloor.hq.app.claim;

import java.util.Locale;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.PeriodNumberingFrom;
import org.springframework.stereotype.Service;

/** The number a claim is given when it is opened. While period numbering is in force (see
 *  {@link PeriodNumberingFrom}) it is <period name>-<n>, counting from 1 in each period, and X-<n> in a
 *  sequence of its own for a claim in no period; otherwise it is the sheet's next sheet-wide number, a
 *  sequence that period numbering leaves as it is. */
@Service
public class ClaimNumbering {

    private final ClaimRepository claims;
    private final PeriodRepository periods;
    private final PeriodNumberingFrom periodNumberingFrom;

    public ClaimNumbering(ClaimRepository claims, PeriodRepository periods, PeriodNumberingFrom periodNumberingFrom) {
        this.claims = claims;
        this.periods = periods;
        this.periodNumberingFrom = periodNumberingFrom;
    }

    public String next(Claim claim) {
        String sheetId = claim.getSheetId();
        if (!periodNumberingFrom.inForce(sheetId)) {
            return String.valueOf(claims.nextNumber(sheetId));
        }
        Period period = periods.findBySheet(sheetId).stream().filter(p -> p.contains(claim.getDate()))
                .findFirst().orElse(null);
        if (period == null) {
            return "X-" + claims.nextInSequence(sheetId, "X");
        }
        return period.name() + "-" + claims.nextInSequence(sheetId, "P:" + period.name().toLowerCase(Locale.ROOT));
    }
}
