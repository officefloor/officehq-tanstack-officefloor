package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The number a claim receives as it is opened. While the sheet's period-numbering-from setting is a date and the
 * sheet's today is on or after it, the claim is numbered within the period containing its date, {@code <period>-<n>}
 * counting from 1 in each period, or {@code X-<n>} in its own sequence when no period contains it. Otherwise it takes
 * the sheet-wide next number (1, 2, ...), which continues where it left off.
 */
public final class ClaimNumbering {

    private ClaimNumbering() {
    }

    public static String next(Claim claim, ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            SheetRepository sheets) {
        String sheet = claim.sheetId();
        if (!perPeriod(sheet, settings, sheets)) {
            return String.valueOf(claims.nextNumber(sheet));
        }
        Period period = periods.containing(sheet, claim.date());
        String key = period == null ? "X" : period.name();
        return key + "-" + claims.nextPeriodNumber(sheet, key);
    }

    /** Whether claims are numbered per period now: the period-numbering-from setting is a date on or before today. */
    public static boolean perPeriod(String sheet, SettingRepository settings, SheetRepository sheets) {
        LocalDate from = settings.periodNumberingFrom(sheet);
        return from != null && !sheets.today(sheet).isBefore(from);
    }
}
