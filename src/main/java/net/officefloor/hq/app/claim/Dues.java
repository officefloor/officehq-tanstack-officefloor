package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A claim's due figure. */
public final class Dues {
    private Dues() {
    }

    /** An amount times a percent, divided by 100, rounded half to even to two decimals. */
    public static String of(String amount, String pct) {
        return new BigDecimal(amount).multiply(new BigDecimal(pct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN).toPlainString();
    }
}
