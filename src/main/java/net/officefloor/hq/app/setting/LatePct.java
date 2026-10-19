package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/** The sheet's late percent (two decimals, 0.00 until set). An overdue claim (OPEN or PENDING, its due-by — fixed
 *  when it was created, see {@link Terms} — before the sheet's today) gains the late percent of its figure for every full
 *  30 days it is overdue, rounded to two decimals by the claim's rounding (see {@link PolicyDate}), on top. The charge is never bound by a
 *  statement; settling freezes it into the settled due. A correction (a negative amount) has no late charge. */
@Service
public class LatePct {

    public static final String KEY = "late-pct";

    public static final String DEFAULT = "0.00";

    private final SettingRepository settings;
    private final SheetClock clock;

    public LatePct(SettingRepository settings, SheetClock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** The late charge as it stands on the sheet's today. */
    public Charge of(String sheetId) {
        return new Charge(new BigDecimal(settings.find(sheetId, KEY).orElse(DEFAULT)), clock.today(sheetId));
    }

    /** The late percent and the day the charge is worked out on. */
    public record Charge(BigDecimal pct, LocalDate today) {

        /** The figure with the claim's late charge, rounded by {@code mode}, on top; null (n/a) stays null. */
        public BigDecimal addTo(Claim claim, BigDecimal figure, RoundingMode mode) {
            if (figure == null || !claim.getStatus().isOpen() || claim.isCorrection()) return figure;
            long late = ChronoUnit.DAYS.between(claim.getDueBy(), today);
            if (late <= 0) return figure;
            long periods = late / 30;
            return figure.add(figure.multiply(pct).multiply(BigDecimal.valueOf(periods))
                    .divide(BigDecimal.valueOf(100), 2, mode));
        }
    }
}
