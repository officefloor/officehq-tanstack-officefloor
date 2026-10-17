package net.officefloor.hq.app.claim;

/** Moves a claim to a new status, refusing a change its current status does not allow. */
final class ClaimTransition {
    private ClaimTransition() {
    }

    static void to(Claim claim, ClaimStatus to) throws InvalidClaimStatusException {
        check(claim.getStatus(), to);
        claim.setStatus(to);
    }

    static void check(ClaimStatus from, ClaimStatus to) throws InvalidClaimStatusException {
        if (!from.canBecome(to)) {
            throw new InvalidClaimStatusException("A " + from + " claim cannot become " + to + ".");
        }
    }
}
