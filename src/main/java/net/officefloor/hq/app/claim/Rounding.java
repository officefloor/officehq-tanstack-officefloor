package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded: a claim dated before
 * the sheet's policy date rounds half up, one dated on or after it half to even. A correction rounds
 * by the date of the claim it corrects, not its own.
 */
public final class Rounding {

    private Rounding() {
    }

    public static RoundingMode of(Claim claim, LocalDate policyDate) {
        return claim.getRoundingDate().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
