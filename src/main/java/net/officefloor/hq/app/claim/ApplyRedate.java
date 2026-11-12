package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's new date, keeping its due-by and any figure a statement bound it to. While the
 *  sheet numbers by period and the new date falls in another period (or none), the claim takes the
 *  next number of its new period's sequence; its old number is not given again. */
public class ApplyRedate {
    public void apply(@Val RedateInput input, @Val Claim claim, ClaimStore store, SettingStore settings,
            SheetStore sheets, PeriodStore periods, Out<Redating> redating) throws InvalidSheetException {
        LocalDate old = claim.getDate();
        String number = claim.getNumber();
        LocalDate from = settings.periodNumberingFrom(claim.getSheetId());
        if (number != null && from != null && !sheets.today(claim.getSheetId()).isBefore(from)
                && !Objects.equals(periodName(periods, claim.getSheetId(), old),
                        periodName(periods, claim.getSheetId(), input.date()))) {
            number = Numbering.next(claim.getSheetId(), input.date(), store, settings, sheets, periods);
        }
        if (!store.redate(claim, input.date(), number)) {
            throw new InvalidSheetException("The claim has moved on");
        }
        redating.set(new Redating(old, input.date()));
    }

    private static String periodName(PeriodStore periods, String sheetId, LocalDate date) {
        PeriodResponse period = periods.on(sheetId, date);
        return period == null ? null : period.name();
    }
}
