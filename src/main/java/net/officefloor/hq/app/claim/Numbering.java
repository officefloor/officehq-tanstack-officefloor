package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** The number a claim receives when opened. While the sheet's period-numbering-from is set and the
 *  sheet's today is on or after it, the claim is numbered within the period its due-by falls in,
 *  <period name>-<n> counting from 1 in each period, or X-<n> in its own sequence when it falls in
 *  none. Otherwise it takes the next sheet-wide number (1, 2, ...) of the sequence all the sheets of
 *  its tree share: one after the last number given on any of them. */
public final class Numbering {

    /** The sequence of claims due by a date in no period. */
    private static final String NO_PERIOD = "X";

    private Numbering() {
    }

    public static String next(String sheetId, LocalDate dueBy, ClaimStore claims, SettingStore settings,
            SheetStore sheets, PeriodStore periods) {
        LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            return String.valueOf(claims.nextNumber(sheetId, settings.wholeTree(sheetId)));
        }
        PeriodResponse period = periods.on(sheetId, dueBy);
        if (period == null) {
            return NO_PERIOD + "-" + claims.nextNumber(sheetId, NO_PERIOD);
        }
        return period.name() + "-" + claims.nextNumber(sheetId, "P:" + period.name());
    }
}
