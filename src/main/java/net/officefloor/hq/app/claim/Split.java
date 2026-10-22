package net.officefloor.hq.app.claim;

/** A stored split: the original claim as left with the rest of its amount, and the new claim split off it. */
public record Split(Claim original, Claim added) {
}
