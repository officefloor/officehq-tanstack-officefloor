package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/** The number a claim gets when it is opened. While the sheet's period numbering from is set and
 *  the sheet's today is on or after it, the claim is numbered within the period its due-by falls in
 *  (<period name>-<n>), or X-<n> when it falls in none; otherwise it takes the sheet's next number. */
@Service
public class ClaimNumbers {

    /** The sequence of claims in no period. */
    public static final String NO_PERIOD = "X";

    private final Sheets sheets;
    private final Settings settings;
    private final Periods periods;

    public ClaimNumbers(Sheets sheets, Settings settings, Periods periods) {
        this.sheets = sheets;
        this.settings = settings;
        this.periods = periods;
    }

    public String next(Claim claim) {
        String sheetId = claim.getSheetId();
        LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            return String.valueOf(sheets.nextClaimNumber(sheetId));
        }
        String seq = periods.onSheet(sheetId).stream().filter(p -> p.contains(claim.getDueBy()))
                .map(Period::name).findFirst().orElse(NO_PERIOD);
        return seq + "-" + sheets.nextPeriodNumber(sheetId, seq);
    }
}
