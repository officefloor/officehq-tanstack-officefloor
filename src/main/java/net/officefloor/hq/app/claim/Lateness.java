package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** What a sheet's late charge follows: each claim's due-by (fixed when it was added), the sheet's late
 *  percent in force on the claim's date and its today; and the sheet's floor in force on each claim's date. An overdue claim (OPEN or PENDING, not deferred, due-by before today) gains the late percent of its
 *  figure for every full 30 days it is overdue, rounded as the sheet's {@link Rounding} has the claim. The charge is never bound by a
 *  statement; settling freezes it into the settled due. */
public record Lateness(Function<LocalDate, BigDecimal> latePcts, Function<LocalDate, BigDecimal> floors,
        LocalDate today, Rounding rounding) {

    /** The days in each step of the late charge. */
    private static final int PERIOD_DAYS = 30;

    public static Lateness of(String sheetId, SettingStore settings, SheetStore sheets, ClaimStore claims) {
        return new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), sheets.today(sheetId),
                Rounding.of(sheetId, settings, sheets, claims));
    }

    /** The late charge on a claim whose figure (bound, else floored live) is {@code figure}; none on
     *  a correction. */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        if (figure == null || !claim.getStatus().isOpen() || today == null || claim.isCorrection()
                || claim.isDeferredOn(today)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDueBy(), today);
        long periods = overdue <= 0 ? 0 : overdue / PERIOD_DAYS;
        return ClaimFigure.of(figure, latePcts.apply(claim.getDate()).multiply(BigDecimal.valueOf(periods)), rounding.mode(claim));
    }

    /** The floor in force on the claim's date. */
    public BigDecimal floor(Claim claim) {
        return floors.apply(claim.getDate());
    }

    /** The claim's due now: its figure, plus the late charge unless settled (its settled due has it). */
    public BigDecimal due(Claim claim, BigDecimal pct) {
        BigDecimal figure = ClaimFigure.of(claim, pct, floor(claim), rounding);
        if (figure == null || claim.getSettledDue() != null) {
            return figure;
        }
        return figure.add(charge(claim, figure));
    }
}
