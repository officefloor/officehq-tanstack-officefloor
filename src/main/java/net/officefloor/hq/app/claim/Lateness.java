package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;

/** What a sheet's late charge follows: its terms (days after a claim's date it is due by), its late
 *  percent and its today. An overdue claim (OPEN or PENDING, due-by before today) gains the late percent of its
 *  figure for every full 30 days it is overdue, rounded as the sheet's {@link Rounding} has the claim. The charge is never bound by a
 *  statement; settling freezes it into the settled due. */
public record Lateness(int terms, BigDecimal pct, LocalDate today, Rounding rounding) {

    /** The days in each step of the late charge. */
    private static final int PERIOD_DAYS = 30;

    public static Lateness of(String sheetId, SettingStore settings, SheetStore sheets, ClaimStore claims) {
        return new Lateness(settings.terms(sheetId), settings.latePct(sheetId), sheets.today(sheetId),
                Rounding.of(sheetId, settings, sheets, claims));
    }

    /** The late charge on a claim whose figure (bound, else floored live) is {@code figure}; none on
     *  a correction. */
    public BigDecimal charge(Claim claim, BigDecimal figure) {
        if (figure == null || !claim.getStatus().isOpen() || today == null || claim.isCorrection()) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDate().plusDays(terms), today);
        long periods = overdue <= 0 ? 0 : overdue / PERIOD_DAYS;
        return ClaimFigure.of(figure, pct.multiply(BigDecimal.valueOf(periods)), rounding.mode(claim));
    }

    /** The claim's due now: its figure, plus the late charge unless settled (its settled due has it). */
    public BigDecimal due(Claim claim, BigDecimal pct, BigDecimal floor) {
        BigDecimal figure = ClaimFigure.of(claim, pct, floor, rounding);
        if (figure == null || claim.getSettledDue() != null) {
            return figure;
        }
        return figure.add(charge(claim, figure));
    }
}
