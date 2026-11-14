package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * How a claim's live figures (the rate step and the late charge) are rounded: a claim dated before the sheet's
 * policy date rounds half up, one dated on or after it half to even — unless a rounding mode is in force on its date
 * ({@code modes}: the mode in force on a date, null: none), which it rounds by instead. A correction rounds by the date
 * of the claim it corrects, not its own; {@code dates} holds the claims' dates by number.
 */
public record Rounding(LocalDate policyDate, Map<String, LocalDate> dates, Function<LocalDate, RoundingMode> modes) {

    /** The rounding by the policy date for the given claims (those a correction may name), with no rounding mode. */
    public static Rounding of(LocalDate policyDate, List<Claim> claims) {
        return of(policyDate, (date) -> null, claims);
    }

    /** The rounding by the rounding mode in force on a claim's date, else by the policy date, for the given claims. */
    public static Rounding of(LocalDate policyDate, Function<LocalDate, RoundingMode> modes, List<Claim> claims) {
        Map<String, LocalDate> dates = new HashMap<>();
        for (Claim claim : claims) {
            if (claim.number() != null) {
                dates.put(claim.number(), claim.date());
            }
        }
        return new Rounding(policyDate, dates, modes);
    }

    /** The date the claim rounds by: its own, or for a correction that of the claim it corrects. */
    public LocalDate dateOf(Claim claim) {
        return claim.correction() ? dates.getOrDefault(claim.corrects(), claim.date()) : claim.date();
    }

    /** How the claim's live figures round. */
    public RoundingMode modeFor(Claim claim) {
        LocalDate date = dateOf(claim);
        RoundingMode mode = modes.apply(date);
        if (mode != null) {
            return mode;
        }
        return date.isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }
}
