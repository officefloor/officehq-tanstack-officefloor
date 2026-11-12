package net.officefloor.hq.app.claim;

/** A stored split: the new OPEN claim split off the claim. */
public record Split(Claim into) {
}
