package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded: by the sheet's rounding mode in force on the
 * claim's date when one is; else a claim dated before the sheet's policy date rounds half up, one dated on or after it half
 * to even. A correction rounds by the date of the claim it corrects, not its own.
 */
public final class Rounding {

    /** The rounding mode setting's values, as typed. */
    public static final String HALF_UP = "half-up", HALF_EVEN = "half-even";

    private Rounding() {
    }

    /** A rounding mode as stored in a dated setting (1: half up, 2: half to even); null for none. */
    public static BigDecimal code(String mode) {
        return HALF_UP.equals(mode) ? BigDecimal.ONE : HALF_EVEN.equals(mode) ? BigDecimal.valueOf(2) : null;
    }

    /** A stored rounding mode's name ("" for none). */
    public static String name(BigDecimal code) {
        return code == null ? "" : code.intValue() == 1 ? HALF_UP : HALF_EVEN;
    }

    /** A stored rounding mode; null for none. */
    public static RoundingMode mode(BigDecimal code) {
        return code == null ? null : code.intValue() == 1 ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** @param modeOn the sheet's rounding mode in force on a date; null for none. */
    public static RoundingMode of(Claim claim, LocalDate policyDate, Function<LocalDate, RoundingMode> modeOn) {
        RoundingMode set = modeOn.apply(claim.getRoundingDate());
        if (set != null) {
            return set;
        }
        return claim.getRoundingDate().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
