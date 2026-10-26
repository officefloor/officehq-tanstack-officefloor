package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.FiscalYear;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** How a claim opened on a sheet now is numbered. */
public final class ClaimNumbering {

    private ClaimNumbering() {
    }

    /**
     * The number a claim due by {@code dueBy} takes when opened on the sheet now: while the sheet numbers by period
     * and its today is on or after that setting's date, the next within the claim's period (by its due-by) as
     * {@code <period name>-<n>}, or {@code X-<n>} in no period; otherwise the tree's next within the fiscal year of its due-by (by the sheet's fiscal
     * start month) as {@code FY<yyyy>-<n>}.
     */
    public static String number(String sheetId, LocalDate dueBy, ClaimRepository repository,
            SettingRepository settings, SheetRepository sheets, PeriodRepository periods) {
        LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            String fy = FiscalYear.of(dueBy, settings.fiscalStartMonth(sheetId)).name();
            return fy + "-" + repository.nextFiscalNumber(sheetId, fy);
        }
        Period period = periods.on(sheetId, dueBy);
        return period == null ? "X-" + repository.nextNumber(sheetId, "X")
                : period.name() + "-" + repository.nextNumber(sheetId, String.valueOf(period.id()));
    }
}
