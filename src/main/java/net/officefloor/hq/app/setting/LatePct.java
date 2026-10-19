package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/** The sheet's late percent (two decimals, 0.00 until set). An overdue claim (OPEN, its due-by — its
 *  date plus the terms — before the sheet's today) gains the late percent of its figure for every full
 *  30 days it is overdue, rounded half to even to two decimals, on top. The charge is never bound by a
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
        return new Charge(new BigDecimal(settings.find(sheetId, KEY).orElse(DEFAULT)),
                Integer.parseInt(settings.find(sheetId, Terms.KEY).orElse(Terms.DEFAULT)), clock.today(sheetId));
    }

    /** The late percent, the terms and the day the charge is worked out on. */
    public record Charge(BigDecimal pct, int terms, LocalDate today) {

        /** The figure with the claim's late charge on top; null (n/a) stays null. */
        public BigDecimal addTo(Claim claim, BigDecimal figure) {
            if (figure == null || claim.getStatus() != ClaimStatus.OPEN || claim.isCorrection()) return figure;
            long late = ChronoUnit.DAYS.between(claim.getDate().plusDays(terms), today);
            if (late <= 0) return figure;
            long periods = late / 30;
            return figure.add(figure.multiply(pct).multiply(BigDecimal.valueOf(periods))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
        }
    }
}
