package net.officefloor.hq.app.claim;

/** The new claim a split made, as stored (the claim it was split from stays the pipeline's Claim). */
public record SplitOff(Claim claim) {
}
