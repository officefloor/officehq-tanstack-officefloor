package net.officefloor.hq.app.claim;

/** A status change that names a known target status. */
public record ValidClaimStatus(ClaimStatus target, String user) {
}
