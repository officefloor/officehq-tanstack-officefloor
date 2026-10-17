package net.officefloor.hq.app.claim;

/** Moves a claim to a new status, refusing a change its current status does not allow. */
final class ClaimTransition {
    private ClaimTransition() {
    }

    static void to(Claim claim, ClaimStatus to) throws InvalidClaimStatusException {
        if (!claim.getStatus().canBecome(to)) {
            throw new InvalidClaimStatusException(
                    "A " + claim.getStatus() + " claim cannot become " + to + ".");
        }
        claim.setStatus(to);
    }
}
