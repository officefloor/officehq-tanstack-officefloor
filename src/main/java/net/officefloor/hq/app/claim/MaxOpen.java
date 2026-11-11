package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The sheet's maximum of open claims per person: while it is set, a person may have at most that many
 * OPEN or PENDING claims, so opening another of theirs is refused.
 */
public final class MaxOpen {
    private MaxOpen() {
    }

    /** Whether the claim's person already has as many OPEN or PENDING claims as the sheet allows. */
    public static boolean full(Sheet sheet, Claim claim, Settings settings, ClaimRepository claims) {
        Integer max = settings.maxOpen();
        if (max == null) return false;
        long held = claims.findByPerson(sheet.id(), claim.getPersonId()).stream()
                .filter(c -> !c.getId().equals(claim.getId()))
                .filter(MaxOpen::counts)
                .count();
        return held >= max;
    }

    /** How many OPEN or PENDING claims the person has. */
    public static long held(Sheet sheet, long personId, ClaimRepository claims) {
        return claims.findByPerson(sheet.id(), personId).stream().filter(MaxOpen::counts).count();
    }

    private static boolean counts(Claim c) {
        return c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING;
    }

    /** Refuses opening the claim when its person is already at the maximum. */
    public static void check(Sheet sheet, Claim claim, Settings settings, ClaimRepository claims)
            throws InvalidClaimStatusException {
        if (full(sheet, claim, settings, claims)) throw refused(settings);
    }

    /** The refusal of opening a claim whose person is at the maximum. */
    public static InvalidClaimStatusException refused(Settings settings) {
        return new InvalidClaimStatusException("A person may have at most " + settings.maxOpen() + " open claims");
    }
}
