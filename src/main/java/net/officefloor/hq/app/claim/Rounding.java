package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded: a claim dated before the sheet's
 * policy date rounds half up, one dated on or after it half to even. A correction rounds by the date of the claim it
 * corrects, not its own; {@code dates} holds the claims' dates by number.
 */
public record Rounding(LocalDate policyDate, Map<String, LocalDate> dates) {

    /** The rounding by the policy date for the given claims (those a correction may name). */
    public static Rounding of(LocalDate policyDate, List<Claim> claims) {
        Map<String, LocalDate> dates = new HashMap<>();
        for (Claim claim : claims) {
            if (claim.number() != null) {
                dates.put(claim.number(), claim.date());
            }
        }
        return new Rounding(policyDate, dates);
    }

    /** The date the claim rounds by: its own, or for a correction that of the claim it corrects. */
    public LocalDate dateOf(Claim claim) {
        return claim.correction() ? dates.getOrDefault(claim.corrects(), claim.date()) : claim.date();
    }

    /** How the claim's live figures round. */
    public RoundingMode modeFor(Claim claim) {
        return dateOf(claim).isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
