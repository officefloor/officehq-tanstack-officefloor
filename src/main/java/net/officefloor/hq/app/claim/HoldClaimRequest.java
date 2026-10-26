package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) holding or releasing a claim. */
public record HoldClaimRequest(String by) {
}
