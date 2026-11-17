package net.officefloor.hq.app.claim;

/** Body of holding or releasing a claim: whether it is to be held (true) or released (false), and who does it. */
public record ClaimHoldRequest(Boolean held, String by) {
}
